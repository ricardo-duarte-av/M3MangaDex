package pt.aguiarvieira.m3mangadex.feature.browse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pt.aguiarvieira.m3mangadex.core.data.BrowseSection
import pt.aguiarvieira.m3mangadex.core.data.MangaRepository
import pt.aguiarvieira.m3mangadex.core.datastore.PreferencesDataSource
import pt.aguiarvieira.m3mangadex.core.model.Manga
import javax.inject.Inject

sealed interface SectionState {
    data object Loading : SectionState

    data class Loaded(
        val items: List<Manga>,
    ) : SectionState

    data object Failed : SectionState
}

data class BrowseUiState(
    val sections: Map<BrowseSection, SectionState> = BrowseSection.entries.associateWith { SectionState.Loading },
    val refreshing: Boolean = false,
    /** For picking titles: the user's chapter languages double as their reading languages. */
    val languages: List<String> = emptyList(),
)

sealed interface Suggestions {
    data object Idle : Suggestions

    data object Loading : Suggestions

    data class Results(
        val query: String,
        val items: List<Manga>,
    ) : Suggestions

    data object Failed : Suggestions
}

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class BrowseViewModel
    @Inject
    constructor(
        private val repository: MangaRepository,
        preferences: PreferencesDataSource,
    ) : ViewModel() {
        private val _state = MutableStateFlow(BrowseUiState())
        val state: StateFlow<BrowseUiState> = _state.asStateFlow()

        private val query = MutableStateFlow("")

        val suggestions: StateFlow<Suggestions> =
            query
                .debounce(SUGGESTION_DEBOUNCE_MILLIS)
                .map(String::trim)
                .distinctUntilChanged()
                .flatMapLatest { text ->
                    if (text.length < MIN_QUERY) {
                        flowOf<Suggestions>(Suggestions.Idle)
                    } else {
                        flow<Suggestions> {
                            emit(Suggestions.Loading)
                            emit(suggestionsFor(text))
                        }
                    }
                }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), Suggestions.Idle)

        init {
            // Rows depend on the languages and ratings; reload them whenever those change.
            viewModelScope.launch {
                preferences.preferences
                    .map { it.chapterLanguages to it.contentRatings }
                    .distinctUntilChanged()
                    .collect { (languages, _) ->
                        _state.update { it.copy(languages = languages) }
                        load()
                    }
            }
        }

        fun onQueryChange(text: String) {
            query.value = text
        }

        fun refresh() {
            viewModelScope.launch {
                _state.update { it.copy(refreshing = true) }
                load()
                _state.update { it.copy(refreshing = false) }
            }
        }

        private suspend fun load() =
            coroutineScope {
                BrowseSection.entries
                    .map { section ->
                        async {
                            val result = runCatchingNonCancel { repository.section(section, limitFor(section)) }
                            _state.update { state ->
                                val previous = state.sections[section]
                                val next =
                                    result.fold(
                                        onSuccess = { SectionState.Loaded(it) },
                                        // A failed refresh keeps what was already on screen.
                                        onFailure = {
                                            if (previous is SectionState.Loaded) previous else SectionState.Failed
                                        },
                                    )
                                state.copy(sections = state.sections + (section to next))
                            }
                        }
                    }.awaitAll()
            }

        private suspend fun suggestionsFor(text: String): Suggestions =
            runCatchingNonCancel { repository.suggestions(text, SUGGESTION_COUNT) }
                .fold({ Suggestions.Results(text, it) }, { Suggestions.Failed })

        private companion object {
            const val SUGGESTION_DEBOUNCE_MILLIS = 300L
            const val STOP_TIMEOUT_MILLIS = 5_000L
            const val MIN_QUERY = 2
            const val SUGGESTION_COUNT = 10
            const val HERO_COUNT = 10
            const val ROW_COUNT = 15

            fun limitFor(section: BrowseSection) = if (section == BrowseSection.PopularNew) HERO_COUNT else ROW_COUNT
        }
    }

/** [runCatching] that lets cancellation through, so a scrolled-away load stops cleanly. */
internal inline fun <T> runCatchingNonCancel(block: () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (
        @Suppress("TooGenericExceptionCaught") e: Exception,
    ) {
        Result.failure(e)
    }
