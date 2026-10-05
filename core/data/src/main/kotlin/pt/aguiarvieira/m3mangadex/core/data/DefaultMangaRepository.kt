package pt.aguiarvieira.m3mangadex.core.data

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import pt.aguiarvieira.m3mangadex.core.datastore.PreferencesDataSource
import pt.aguiarvieira.m3mangadex.core.model.AtHomeServer
import pt.aguiarvieira.m3mangadex.core.model.Chapter
import pt.aguiarvieira.m3mangadex.core.model.ContentRating
import pt.aguiarvieira.m3mangadex.core.model.Manga
import pt.aguiarvieira.m3mangadex.core.model.MangaFilter
import pt.aguiarvieira.m3mangadex.core.model.MangaOrder
import pt.aguiarvieira.m3mangadex.core.model.MangaStats
import pt.aguiarvieira.m3mangadex.core.model.Tag
import pt.aguiarvieira.m3mangadex.core.network.MangaDexApi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DefaultMangaRepository
    @Inject
    constructor(
        private val api: MangaDexApi,
        private val preferences: PreferencesDataSource,
    ) : MangaRepository {
        private val tagsLock = Mutex()
        private var tags: List<Tag>? = null
        private val mangaCache = ExpiringCache<String, Manga>(CACHE_SIZE, CACHE_TTL_MILLIS)
        private val chapterCache =
            ExpiringCache<Pair<String, List<String>>, List<Chapter>>(CACHE_SIZE, CACHE_TTL_MILLIS)

        override suspend fun section(
            section: BrowseSection,
            limit: Int,
        ): List<Manga> {
            val prefs = preferences.preferences.first()
            return api.searchManga(section.filter(prefs.chapterLanguages).withRatings(), 0, limit).items
        }

        override fun search(filter: MangaFilter): Flow<PagingData<Manga>> =
            Pager(PagingConfig(pageSize = PAGE_SIZE, enablePlaceholders = false)) {
                MangaPagingSource { offset, limit -> api.searchManga(filter.withRatings(), offset, limit) }
            }.flow

        /**
         * MangaDex's relevance ranks by the main title, which buries popular series whose main title
         * is romanized ("solo" doesn't surface "Na Honjaman Level-Up" — Solo Leveling). Lead with
         * the few most-followed matches, then relevance.
         */
        override suspend fun suggestions(
            title: String,
            limit: Int,
        ): List<Manga> =
            coroutineScope {
                val filter = MangaFilter(title = title).withRatings()
                val popular =
                    async { api.searchManga(filter.copy(order = MangaOrder.Follows), 0, POPULAR_SUGGESTIONS).items }
                val relevant = async { api.searchManga(filter.copy(order = MangaOrder.Relevance), 0, limit).items }
                (popular.await() + relevant.await()).distinctBy { it.id }.take(limit)
            }

        override suspend fun manga(id: String): Manga = mangaCache.getOrPut(id) { api.manga(id) }

        override suspend fun stats(id: String): MangaStats? = api.statistics(listOf(id))[id]

        override suspend fun chapters(
            mangaId: String,
            languages: List<String>,
        ): List<Chapter> = chapterCache.getOrPut(mangaId to languages) { fetchChapters(mangaId, languages) }

        override suspend fun chapter(id: String): Chapter = api.chapter(id)

        override suspend fun atHomeServer(chapterId: String): AtHomeServer = api.atHomeServer(chapterId)

        private suspend fun fetchChapters(
            mangaId: String,
            languages: List<String>,
        ): List<Chapter> {
            val chapters = mutableListOf<Chapter>()
            var offset: Int? = 0
            while (offset != null && offset < MangaDexApi.MAX_RESULTS) {
                val limit = minOf(FEED_PAGE, MangaDexApi.MAX_RESULTS - offset)
                // Every rating: the user already chose to open this manga.
                val page = api.chapters(mangaId, languages, ContentRating.entries, offset, limit)
                chapters += page.items
                offset = page.nextOffset
            }
            return chapters
        }

        override suspend fun tags(): List<Tag> =
            tagsLock.withLock {
                tags ?: api.tags().sortedBy { it.name }.also { tags = it }
            }

        private suspend fun MangaFilter.withRatings(): MangaFilter =
            if (contentRating.isNotEmpty()) {
                this
            } else {
                copy(
                    contentRating = preferences.preferences.first().contentRatings
                )
            }

        private companion object {
            const val PAGE_SIZE = 30
            const val FEED_PAGE = 500
            const val POPULAR_SUGGESTIONS = 3
            const val CACHE_SIZE = 20
            const val CACHE_TTL_MILLIS = 5 * 60 * 1000L
        }
    }
