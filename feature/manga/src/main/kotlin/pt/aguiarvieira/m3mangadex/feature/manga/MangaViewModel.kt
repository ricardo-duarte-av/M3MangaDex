package pt.aguiarvieira.m3mangadex.feature.manga

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pt.aguiarvieira.m3mangadex.core.auth.AuthRepository
import pt.aguiarvieira.m3mangadex.core.auth.Session
import pt.aguiarvieira.m3mangadex.core.data.ChapterLanguages
import pt.aguiarvieira.m3mangadex.core.data.LibraryRepository
import pt.aguiarvieira.m3mangadex.core.data.MangaRepository
import pt.aguiarvieira.m3mangadex.core.data.ReadingRepository
import pt.aguiarvieira.m3mangadex.core.data.download.DownloadRepository
import pt.aguiarvieira.m3mangadex.core.datastore.PreferencesDataSource
import pt.aguiarvieira.m3mangadex.core.model.Chapter
import pt.aguiarvieira.m3mangadex.core.model.ChapterOrder
import pt.aguiarvieira.m3mangadex.core.model.ChapterProgress
import pt.aguiarvieira.m3mangadex.core.model.Download
import pt.aguiarvieira.m3mangadex.core.model.Manga
import pt.aguiarvieira.m3mangadex.core.model.MangaStats
import pt.aguiarvieira.m3mangadex.core.model.ReadingStatus
import pt.aguiarvieira.m3mangadex.core.model.neighbors

sealed interface ChaptersState {
    data object Loading : ChaptersState

    data class Loaded(
        val volumes: List<VolumeGroup>,
        val all: List<Chapter>,
    ) : ChaptersState {
        val count: Int get() = all.size
    }

    data object Failed : ChaptersState
}

sealed interface MangaUiState {
    data object Loading : MangaUiState

    data object Failed : MangaUiState

    data class Loaded(
        val manga: Manga,
        val stats: MangaStats?,
        val chapters: ChaptersState,
        val languages: List<String>,
    ) : MangaUiState
}

/** Languages this manga has chapters in beyond the user's global ones, and which are switched on. */
data class OtherLanguages(
    val available: List<String> = emptyList(),
    val selected: Set<String> = emptySet(),
)

/** The manga in the user's MangaDex account (all defaults when logged out). */
data class AccountState(
    val loggedIn: Boolean = false,
    val status: ReadingStatus? = null,
    val following: Boolean = false,
    /** Chapters marked read on MangaDex, from any device. */
    val remoteRead: Set<String> = emptySet(),
    /** Bumped each time a change couldn't be saved to MangaDex (and was rolled back). */
    val syncFailures: Int = 0,
)

/** Where the user is in this manga: per-chapter progress and what the main button opens. */
data class ReadingState(
    val progress: Map<String, ChapterProgress> = emptyMap(),
    /** Read here or on MangaDex. */
    val read: Set<String> = emptySet(),
    /** The chapter "Start/Continue reading" opens. */
    val resume: Chapter? = null,
    /** True once anything of this manga was read: the button says "Continue". */
    val started: Boolean = false,
)

@HiltViewModel(assistedFactory = MangaViewModel.Factory::class)
class MangaViewModel
    @AssistedInject
    constructor(
        @Assisted private val mangaId: String,
        private val repository: MangaRepository,
        private val readingRepository: ReadingRepository,
        private val library: LibraryRepository,
        private val downloadRepository: DownloadRepository,
        auth: AuthRepository,
        chapterLanguages: ChapterLanguages,
        preferences: PreferencesDataSource,
    ) : ViewModel() {
        // Start from the list's copy when there is one: the header is there from the first frame.
        private val _state =
            MutableStateFlow(
                repository.preview(mangaId)?.let { manga ->
                    MangaUiState.Loaded(
                        manga,
                        null,
                        ChaptersState.Loading,
                        preferences.current.value
                            ?.chapterLanguages
                            .orEmpty()
                    )
                } ?: MangaUiState.Loading,
            )
        val state: StateFlow<MangaUiState> = _state.asStateFlow()

        private val _account = MutableStateFlow(AccountState())
        val account: StateFlow<AccountState> = _account.asStateFlow()

        val reading: StateFlow<ReadingState> =
            combine(
                _state,
                readingRepository.progress(mangaId),
                readingRepository.lastRead(mangaId),
                _account,
            ) { state, progress, last, account ->
                val chapters = ((state as? MangaUiState.Loaded)?.chapters as? ChaptersState.Loaded)?.all.orEmpty()
                val read =
                    progress.values
                        .filter { it.isRead }
                        .map { it.chapterId }
                        .toSet() + account.remoteRead
                ReadingState(
                    progress = progress,
                    read = read,
                    resume = resumeChapter(chapters, last, account.remoteRead),
                    started = last != null || account.remoteRead.isNotEmpty(),
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), ReadingState())

        val otherLanguages: StateFlow<OtherLanguages> =
            combine(
                _state,
                preferences.preferences.map { it.chapterLanguages },
                readingRepository.extraLanguages(mangaId),
            ) { state, global, extra ->
                val available = (state as? MangaUiState.Loaded)?.manga?.availableLanguages.orEmpty()
                OtherLanguages(available.filterNot { it in global }.sorted(), extra.toSet())
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), OtherLanguages())

        val downloads: StateFlow<Map<String, Download>> =
            downloadRepository
                .forManga(
                    mangaId
                ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyMap())

        fun download(chapters: List<Chapter>) {
            val manga = (_state.value as? MangaUiState.Loaded)?.manga ?: return
            viewModelScope.launch { downloadRepository.enqueue(manga, chapters) }
        }

        fun deleteDownload(chapterId: String) {
            viewModelScope.launch { downloadRepository.delete(chapterId) }
        }

        fun retryDownload(chapterId: String) {
            viewModelScope.launch { downloadRepository.retry(chapterId) }
        }

        private var loading: Job? = null
        private var languages: List<String>? = null

        init {
            viewModelScope.launch {
                auth.session.map { it is Session.LoggedIn }.distinctUntilChanged().collect { loggedIn ->
                    if (loggedIn) loadAccount() else _account.value = AccountState()
                }
            }
            viewModelScope.launch {
                library.statuses.collect { statuses -> _account.update { it.copy(status = statuses[mangaId]) } }
            }
            // Chapters follow the global and this manga's extra languages; reload when they change.
            viewModelScope.launch {
                chapterLanguages.forManga(mangaId).collect {
                    languages = it
                    load(it)
                }
            }
        }

        /** Shows (or hides again) this manga's chapters in [language]. */
        fun toggleLanguage(language: String) {
            viewModelScope.launch {
                val current = otherLanguages.value.selected
                val next = if (language in current) current - language else current + language
                readingRepository.setExtraLanguages(mangaId, next.toList())
            }
        }

        /** Files the manga under [status] (which also follows it), or takes it out of the library. */
        fun setStatus(status: ReadingStatus?) {
            val before = _account.value
            _account.update { it.copy(status = status, following = status != null) }
            viewModelScope.launch {
                if (attempt { library.setStatus(mangaId, status) }?.isFailure != false) {
                    _account.update { before.copy(syncFailures = it.syncFailures + 1) }
                }
            }
        }

        fun toggleFollow() {
            val following = !_account.value.following
            _account.update { it.copy(following = following) }
            viewModelScope.launch {
                if (attempt { library.setFollowing(mangaId, following) }?.isFailure != false) {
                    _account.update { it.copy(following = !following, syncFailures = it.syncFailures + 1) }
                }
            }
        }

        /** Marks [chapter] read (or unread again) here and, when logged in, on MangaDex. */
        fun toggleRead(chapter: Chapter) {
            val read = chapter.id !in reading.value.read
            viewModelScope.launch {
                if (read) {
                    val pages = chapter.pages.coerceAtLeast(1)
                    readingRepository.saveProgress(mangaId, chapter, pages - 1, pages)
                } else {
                    readingRepository.clearProgress(chapter.id)
                }
                if (_account.value.loggedIn) {
                    _account.update {
                        it.copy(
                            remoteRead =
                                if (read) {
                                    it.remoteRead + chapter.id
                                } else {
                                    it.remoteRead -
                                        chapter.id
                                }
                        )
                    }
                    attempt { library.setRead(mangaId, listOf(chapter.id), read) }
                }
            }
        }

        private suspend fun loadAccount() {
            _account.update { it.copy(loggedIn = true) }
            coroutineScope {
                launch { attempt { library.refreshStatuses() } }
                launch {
                    attempt { library.isFollowing(mangaId) }?.getOrNull()?.let { f ->
                        _account.update { it.copy(following = f) }
                    }
                }
                launch {
                    attempt { library.readChapters(mangaId) }?.getOrNull()?.let { r ->
                        _account.update { it.copy(remoteRead = r) }
                    }
                }
            }
        }

        fun retry() {
            languages?.let(::load)
        }

        private fun load(languages: List<String>) {
            loading?.cancel()
            loading =
                viewModelScope.launch {
                    val chapters = async { attempt { repository.chapters(mangaId, languages) } }
                    val stats = async { attempt { repository.stats(mangaId) }?.getOrNull() }
                    val manga = attempt { repository.manga(mangaId) }?.getOrNull()
                    if (manga == null) {
                        _state.value = MangaUiState.Failed
                        return@launch
                    }
                    // A reload (languages changed) keeps the current list until the new one is in, so the
                    // screen doesn't collapse and lose its scroll position.
                    val shown = _state.value as? MangaUiState.Loaded
                    if (shown?.chapters !is ChaptersState.Loaded) {
                        _state.value = MangaUiState.Loaded(manga, shown?.stats, ChaptersState.Loading, languages)
                    }
                    val loadedStats = stats.await() ?: shown?.stats
                    val chapterState =
                        chapters.await()?.getOrNull()?.let { list -> ChaptersState.Loaded(groupByVolume(list), list) }
                            ?: ChaptersState.Failed
                    _state.value = MangaUiState.Loaded(manga, loadedStats, chapterState, languages)
                }
        }

        private suspend fun <T> attempt(block: suspend () -> T): Result<T>? =
            try {
                Result.success(block())
            } catch (e: CancellationException) {
                throw e
            } catch (
                @Suppress("TooGenericExceptionCaught") e: Exception,
            ) {
                Result.failure(e)
            }

        @AssistedFactory
        interface Factory {
            fun create(mangaId: String): MangaViewModel
        }

        private companion object {
            const val STOP_TIMEOUT_MILLIS = 5_000L
        }
    }

/**
 * Nothing read yet: the first chapter, or the one after the furthest read on MangaDex. Otherwise
 * the last one opened, or the one after it if it was finished (staying on it when it's the latest).
 */
internal fun resumeChapter(
    chapters: List<Chapter>,
    last: ChapterProgress?,
    remoteRead: Set<String> = emptySet(),
): Chapter? {
    val lastChapter = last?.let { progress -> chapters.firstOrNull { it.id == progress.chapterId } }
    // Read on another device (MangaDex's markers) but never opened here: carry on after the furthest.
    val furthestRemote = chapters.filter { it.id in remoteRead }.maxWithOrNull(ChapterOrder)
    return when {
        lastChapter == null && furthestRemote != null -> neighbors(chapters, furthestRemote).next ?: furthestRemote
        lastChapter == null -> firstReadable(chapters)
        last.isRead -> neighbors(chapters, lastChapter).next ?: lastChapter
        else -> lastChapter
    }
}
