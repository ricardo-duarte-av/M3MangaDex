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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import pt.aguiarvieira.m3mangadex.core.data.MangaRepository
import pt.aguiarvieira.m3mangadex.core.datastore.PreferencesDataSource
import pt.aguiarvieira.m3mangadex.core.model.Chapter
import pt.aguiarvieira.m3mangadex.core.model.Manga
import pt.aguiarvieira.m3mangadex.core.model.MangaStats

sealed interface ChaptersState {
    data object Loading : ChaptersState

    data class Loaded(
        val volumes: List<VolumeGroup>,
        val count: Int,
        val first: Chapter?,
    ) : ChaptersState

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

@HiltViewModel(assistedFactory = MangaViewModel.Factory::class)
class MangaViewModel
    @AssistedInject
    constructor(
        @Assisted private val mangaId: String,
        private val repository: MangaRepository,
        preferences: PreferencesDataSource,
    ) : ViewModel() {
        private val _state = MutableStateFlow<MangaUiState>(MangaUiState.Loading)
        val state: StateFlow<MangaUiState> = _state.asStateFlow()

        private var loading: Job? = null
        private var languages: List<String>? = null

        init {
            // Chapters follow the chapter-language setting; reload when it changes.
            viewModelScope.launch {
                preferences.preferences.map { it.chapterLanguages }.distinctUntilChanged().collect {
                    languages = it
                    load(it)
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
                    _state.value = MangaUiState.Loaded(manga, null, ChaptersState.Loading, languages)
                    val loadedStats = stats.await()
                    val chapterState =
                        chapters.await()?.getOrNull()?.let { list ->
                            ChaptersState.Loaded(groupByVolume(list), list.size, firstReadable(list))
                        } ?: ChaptersState.Failed
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
    }
