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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pt.aguiarvieira.m3mangadex.core.data.ChapterLanguages
import pt.aguiarvieira.m3mangadex.core.data.MangaRepository
import pt.aguiarvieira.m3mangadex.core.data.ReadingRepository
import pt.aguiarvieira.m3mangadex.core.datastore.PreferencesDataSource
import pt.aguiarvieira.m3mangadex.core.model.Chapter
import pt.aguiarvieira.m3mangadex.core.model.ChapterProgress
import pt.aguiarvieira.m3mangadex.core.model.Manga
import pt.aguiarvieira.m3mangadex.core.model.MangaStats
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

/** Where the user is in this manga: per-chapter progress and what the main button opens. */
data class ReadingState(
    val progress: Map<String, ChapterProgress> = emptyMap(),
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
        chapterLanguages: ChapterLanguages,
        preferences: PreferencesDataSource,
    ) : ViewModel() {
        private val _state = MutableStateFlow<MangaUiState>(MangaUiState.Loading)
        val state: StateFlow<MangaUiState> = _state.asStateFlow()

        val reading: StateFlow<ReadingState> =
            combine(
                _state,
                readingRepository.progress(mangaId),
                readingRepository.lastRead(mangaId)
            ) { state, progress, last ->
                val chapters = ((state as? MangaUiState.Loaded)?.chapters as? ChaptersState.Loaded)?.all.orEmpty()
                ReadingState(progress, resumeChapter(chapters, last), started = last != null)
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

        private var loading: Job? = null
        private var languages: List<String>? = null

        init {
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
 * Nothing read yet: the first chapter. Otherwise the last one opened, or the one after it if it
 * was finished (staying on it when it's the latest there is).
 */
internal fun resumeChapter(
    chapters: List<Chapter>,
    last: ChapterProgress?,
): Chapter? {
    val lastChapter = last?.let { progress -> chapters.firstOrNull { it.id == progress.chapterId } }
    return when {
        lastChapter == null -> firstReadable(chapters)
        last.isRead -> neighbors(chapters, lastChapter).next ?: lastChapter
        else -> lastChapter
    }
}
