package pt.aguiarvieira.m3mangadex.core.data

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.PagingState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import pt.aguiarvieira.m3mangadex.core.datastore.PreferencesDataSource
import pt.aguiarvieira.m3mangadex.core.model.FeedEntry
import pt.aguiarvieira.m3mangadex.core.model.Manga
import pt.aguiarvieira.m3mangadex.core.model.ReadingStatus
import pt.aguiarvieira.m3mangadex.core.network.MangaDexApi
import pt.aguiarvieira.m3mangadex.core.network.MangaDexUserApi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DefaultLibraryRepository
    @Inject
    constructor(
        private val userApi: MangaDexUserApi,
        private val api: MangaDexApi,
        private val preferences: PreferencesDataSource,
        private val mangaRepository: DefaultMangaRepository,
    ) : LibraryRepository {
        private val _statuses = MutableStateFlow<Map<String, ReadingStatus>>(emptyMap())
        override val statuses: StateFlow<Map<String, ReadingStatus>> = _statuses.asStateFlow()

        /** Library manga don't change often; keep them for the session so the tab opens instantly. */
        private val mangaCache = ExpiringCache<String, Manga>(LIBRARY_CACHE_SIZE, LIBRARY_TTL_MILLIS)

        override suspend fun refreshStatuses() {
            _statuses.value = userApi.statuses()
        }

        override suspend fun library(): List<LibraryEntry> {
            refreshStatuses()
            val statuses = _statuses.value
            val missing = statuses.keys.filter { mangaCache[it] == null }
            // Only what the user's (and this build's) ratings allow, even for their own library.
            api.mangaByIds(missing, preferences.preferences.first().contentRatings).forEach { mangaCache[it.id] = it }
            return statuses
                .mapNotNull { (id, status) -> mangaCache[id]?.let { LibraryEntry(it, status) } }
                .also { entries -> mangaRepository.rememberPreviews(entries.map { it.manga }) }
        }

        override suspend fun setStatus(
            mangaId: String,
            status: ReadingStatus?,
        ) {
            userApi.setStatus(mangaId, status)
            // Like the site: a manga in the library is followed (its new chapters show in Updates);
            // taking it out stops following.
            userApi.setFollowing(mangaId, status != null)
            _statuses.update { if (status == null) it - mangaId else it + (mangaId to status) }
        }

        override suspend fun isFollowing(mangaId: String): Boolean = userApi.isFollowing(mangaId)

        override suspend fun setFollowing(
            mangaId: String,
            following: Boolean,
        ) = userApi.setFollowing(mangaId, following)

        override suspend fun readChapters(mangaId: String): Set<String> = userApi.readChapters(mangaId)

        override suspend fun setRead(
            mangaId: String,
            chapterIds: List<String>,
            read: Boolean,
        ) = userApi.markChapters(
            mangaId,
            read = if (read) chapterIds else emptyList(),
            unread = if (read) emptyList() else chapterIds
        )

        override fun updates(): Flow<PagingData<FeedEntry>> =
            Pager(PagingConfig(pageSize = FEED_PAGE, enablePlaceholders = false)) { FeedPagingSource() }.flow

        override fun clear() {
            _statuses.value = emptyMap()
        }

        /** The follows feed, with each page's manga covers filled in (the feed only includes titles). */
        private inner class FeedPagingSource : PagingSource<Int, FeedEntry>() {
            override suspend fun load(params: LoadParams<Int>): LoadResult<Int, FeedEntry> {
                val offset = params.key ?: 0
                return try {
                    val prefs = preferences.preferences.first()
                    val page =
                        userApi.followsFeed(
                            prefs.chapterLanguages,
                            prefs.contentRatings,
                            offset,
                            params.loadSize
                        )
                    val ids = page.items.mapNotNull { it.chapter.mangaId }.filter { mangaCache[it] == null }
                    api.mangaByIds(ids, prefs.contentRatings).forEach { mangaCache[it.id] = it }
                    val entries =
                        page.items.map { entry ->
                            entry.copy(
                                manga =
                                    entry.chapter.mangaId?.let { mangaCache[it] } ?: entry.manga
                            )
                        }
                    LoadResult.Page(
                        entries,
                        prevKey = null,
                        nextKey =
                            page.nextOffset?.takeIf {
                                it <
                                    MangaDexApi.MAX_RESULTS
                            }
                    )
                } catch (e: CancellationException) {
                    throw e
                } catch (
                    @Suppress("TooGenericExceptionCaught") e: Exception,
                ) {
                    LoadResult.Error(e)
                }
            }

            override fun getRefreshKey(state: PagingState<Int, FeedEntry>): Int? = null
        }

        private companion object {
            const val FEED_PAGE = 50
            const val LIBRARY_CACHE_SIZE = 500
            const val LIBRARY_TTL_MILLIS = 30 * 60 * 1000L
        }
    }
