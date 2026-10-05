package pt.aguiarvieira.m3mangadex.feature.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import pt.aguiarvieira.m3mangadex.core.auth.AuthRepository
import pt.aguiarvieira.m3mangadex.core.auth.Session
import pt.aguiarvieira.m3mangadex.core.data.ChapterLanguages
import pt.aguiarvieira.m3mangadex.core.data.LibraryRepository
import pt.aguiarvieira.m3mangadex.core.data.MangaRepository
import pt.aguiarvieira.m3mangadex.core.data.ReadingRepository
import pt.aguiarvieira.m3mangadex.core.data.download.DownloadRepository
import pt.aguiarvieira.m3mangadex.core.datastore.PreferencesDataSource
import pt.aguiarvieira.m3mangadex.core.model.AtHomeServer
import pt.aguiarvieira.m3mangadex.core.model.Chapter
import pt.aguiarvieira.m3mangadex.core.model.ChapterNeighbors
import pt.aguiarvieira.m3mangadex.core.model.Manga
import pt.aguiarvieira.m3mangadex.core.model.PageFit
import pt.aguiarvieira.m3mangadex.core.model.ReaderMode
import pt.aguiarvieira.m3mangadex.core.model.neighbors

sealed interface PagesState {
    data object Loading : PagesState

    data object Failed : PagesState

    data class Loaded(
        val urls: List<String>,
        /** Bumped whenever the page server is re-fetched, so failed images load again. */
        val generation: Int,
    ) : PagesState
}

data class ReaderUiState(
    val manga: Manga? = null,
    val chapter: Chapter? = null,
    val neighbors: ChapterNeighbors = ChapterNeighbors(null, null),
    val pages: PagesState = PagesState.Loading,
    /** The mode the user picked for this manga, if any. */
    val savedMode: ReaderMode? = null,
    /** Where to open: the saved page of a chapter left half-read, else the first. */
    val startPage: Int = 0,
    val failedPages: Set<Int> = emptySet(),
    val doublePageSpreads: Boolean = true,
    val volumeKeyPaging: Boolean = false,
    val pageFit: PageFit = PageFit.Auto,
    val cropBorders: Boolean = true,
    val languages: List<String> = emptyList(),
    /** Reading a downloaded copy. */
    val offline: Boolean = false,
) {
    /** The user's choice for this manga, else its natural mode (right-to-left for manga, …). */
    val mode: ReaderMode get() = savedMode ?: manga?.let(ReaderMode::defaultFor) ?: ReaderMode.LeftToRight
}

@HiltViewModel(assistedFactory = ReaderViewModel.Factory::class)
class ReaderViewModel
    @AssistedInject
    constructor(
        @Assisted("manga") private val mangaId: String,
        @Assisted("chapter") private val chapterId: String,
        private val repository: MangaRepository,
        private val reading: ReadingRepository,
        private val chapterLanguages: ChapterLanguages,
        private val preferences: PreferencesDataSource,
        private val auth: AuthRepository,
        private val library: LibraryRepository,
        private val downloads: DownloadRepository,
    ) : ViewModel() {
        private val _state = MutableStateFlow(ReaderUiState())
        val state: StateFlow<ReaderUiState> = _state.asStateFlow()

        private var server: AtHomeServer? = null
        private var dataSaver = false
        private var generation = 0
        private var lastRefreshMillis = 0L
        private val refreshLock = Mutex()

        init {
            load()
            viewModelScope.launch {
                preferences.preferences.collect { prefs ->
                    dataSaver = prefs.dataSaver
                    _state.update {
                        it.copy(
                            doublePageSpreads = prefs.doublePageSpreads,
                            volumeKeyPaging = prefs.volumeKeyPaging,
                            pageFit = prefs.pageFit,
                            cropBorders = prefs.cropBorders,
                            languages = prefs.chapterLanguages,
                        )
                    }
                    publishPages()
                }
            }
            viewModelScope.launch {
                reading.readerMode(mangaId).collect { saved -> _state.update { it.copy(savedMode = saved) } }
            }
        }

        fun load() {
            viewModelScope.launch {
                _state.update { it.copy(pages = PagesState.Loading) }
                // A downloaded chapter reads from its files, online or not.
                val download = downloads.download(chapterId)
                localPages = downloads.pages(chapterId)?.map { it.toURI().toString() }
                val languages = chapterLanguages.forManga(mangaId).first()
                val manga = attempt { repository.manga(mangaId) }
                val chapters = attempt { repository.chapters(mangaId, languages) }.orEmpty()
                val chapter =
                    chapters.firstOrNull { it.id == chapterId }
                        ?: localPages?.let { download?.toChapter() }
                        ?: attempt { repository.chapter(chapterId) }
                if (localPages == null) server = attempt { repository.atHomeServer(chapterId) }
                val pageCount = pageCount()
                if (chapter == null || pageCount == 0) {
                    _state.update { it.copy(manga = manga, chapter = chapter, pages = PagesState.Failed) }
                    return@launch
                }
                val saved = reading.progressOf(chapterId)
                val start = saved?.takeUnless { it.isRead }?.page?.coerceIn(0, pageCount - 1) ?: 0
                _state.update {
                    it.copy(
                        manga = manga,
                        chapter = chapter,
                        neighbors = neighbors(chapters, chapter),
                        startPage = start,
                        offline = localPages != null,
                        // Read it now rather than wait for the collector, so the first frame of pages
                        // is already in the right mode.
                        savedMode = reading.readerMode(mangaId).first(),
                    )
                }
                publishPages()
            }
        }

        private var localPages: List<String>? = null

        private fun pageCount(): Int = localPages?.size ?: server?.pageCount ?: 0

        /** The page (last of a spread) now on screen; saved as progress. */
        fun onPageShow(page: Int) {
            val chapter = _state.value.chapter ?: return
            val count = pageCount().takeIf { it > 0 } ?: return
            viewModelScope.launch { reading.saveProgress(mangaId, chapter, page.coerceIn(0, count - 1), count) }
            // Reaching the last page marks the chapter read on MangaDex too (and in its history).
            if (page >= count - 1 && !syncedRead && auth.session.value is Session.LoggedIn) {
                syncedRead = true
                viewModelScope.launch {
                    if (attempt { library.setRead(mangaId, listOf(chapter.id), read = true) } ==
                        null
                    ) {
                        syncedRead = false
                    }
                }
            }
        }

        private var syncedRead = false

        /**
         * A page image failed. The node may have expired or broken: ask for a fresh one (at most
         * every few seconds).
         */
        fun onPageFail(page: Int) {
            _state.update { it.copy(failedPages = it.failedPages + page) }
            if (localPages == null) viewModelScope.launch { refreshServer(force = false) }
        }

        fun retryPage(page: Int) {
            _state.update { it.copy(failedPages = it.failedPages - page) }
            viewModelScope.launch { refreshServer(force = true) }
        }

        fun setMode(mode: ReaderMode) {
            _state.update { it.copy(savedMode = mode) }
            viewModelScope.launch { reading.setReaderMode(mangaId, mode) }
        }

        fun setDoublePageSpreads(enabled: Boolean) {
            viewModelScope.launch { preferences.setDoublePageSpreads(enabled) }
        }

        fun setPageFit(fit: PageFit) {
            viewModelScope.launch { preferences.setPageFit(fit) }
        }

        fun setCropBorders(enabled: Boolean) {
            viewModelScope.launch { preferences.setCropBorders(enabled) }
        }

        fun setVolumeKeyPaging(enabled: Boolean) {
            viewModelScope.launch { preferences.setVolumeKeyPaging(enabled) }
        }

        private suspend fun refreshServer(force: Boolean) =
            refreshLock.withLock {
                val now = System.currentTimeMillis()
                if (!force && now - lastRefreshMillis < REFRESH_INTERVAL_MILLIS) return@withLock
                lastRefreshMillis = now
                attempt { repository.atHomeServer(chapterId) }?.let { server = it }
                generation++
                if (force) _state.update { it.copy(failedPages = emptySet()) }
                publishPages()
            }

        private fun publishPages() {
            val local = localPages
            val urls =
                when {
                    local != null -> local
                    else -> server?.let { server -> List(server.pageCount) { server.pageUrl(it, dataSaver) } } ?: return
                }
            _state.update { it.copy(pages = PagesState.Loaded(urls, generation)) }
        }

        private suspend fun <T> attempt(block: suspend () -> T): T? =
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (
                @Suppress("TooGenericExceptionCaught") _: Exception,
            ) {
                null
            }

        @AssistedFactory
        interface Factory {
            fun create(
                @Assisted("manga") mangaId: String,
                @Assisted("chapter") chapterId: String,
            ): ReaderViewModel
        }

        private companion object {
            const val REFRESH_INTERVAL_MILLIS = 5_000L
        }
    }
