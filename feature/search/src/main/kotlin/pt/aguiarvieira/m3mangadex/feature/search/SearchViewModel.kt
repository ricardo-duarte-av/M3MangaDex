package pt.aguiarvieira.m3mangadex.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pt.aguiarvieira.m3mangadex.core.data.BrowseSection
import pt.aguiarvieira.m3mangadex.core.data.MangaRepository
import pt.aguiarvieira.m3mangadex.core.datastore.PreferencesDataSource
import pt.aguiarvieira.m3mangadex.core.model.ContentPolicy
import pt.aguiarvieira.m3mangadex.core.model.ContentRating
import pt.aguiarvieira.m3mangadex.core.model.Manga
import pt.aguiarvieira.m3mangadex.core.model.MangaFilter
import pt.aguiarvieira.m3mangadex.core.model.MangaOrder
import pt.aguiarvieira.m3mangadex.core.model.Tag

/** What search was opened with: a typed query, a Browse row's "See all", or a tag from a manga. */
data class SearchArgs(
    val query: String = "",
    val section: BrowseSection? = null,
    val tagId: String? = null,
)

sealed interface TagsState {
    data object Loading : TagsState

    data class Loaded(
        val tags: List<Tag>,
    ) : TagsState

    data object Failed : TagsState
}

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel(assistedFactory = SearchViewModel.Factory::class)
class SearchViewModel
    @AssistedInject
    constructor(
        @Assisted args: SearchArgs,
        private val repository: MangaRepository,
        preferences: PreferencesDataSource,
        policy: ContentPolicy,
    ) : ViewModel() {
        /** The ratings this build lets the user filter by. */
        val selectableRatings: List<ContentRating> = policy.selectable

        private val _filter = MutableStateFlow(initialFilter(args))

        /** The filters and sort; the title lives in [title] so typing can be debounced on its own. */
        val filter: StateFlow<MangaFilter> = _filter.asStateFlow()

        private val title = MutableStateFlow(args.query)

        /** Restrict results to titles with chapters in the user's languages (as the Browse rows are). */
        private val _onlyMyLanguages = MutableStateFlow(true)
        val onlyMyLanguages: StateFlow<Boolean> = _onlyMyLanguages.asStateFlow()

        private val _tags = MutableStateFlow<TagsState>(TagsState.Loading)
        val tags: StateFlow<TagsState> = _tags.asStateFlow()

        val languages: StateFlow<List<String>> =
            preferences.preferences
                .map { it.chapterLanguages }
                .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

        val results: Flow<PagingData<Manga>> =
            combine(
                title.debounce(TYPING_DEBOUNCE_MILLIS).map(String::trim).distinctUntilChanged(),
                _filter,
                _onlyMyLanguages,
                preferences.preferences.map { it.chapterLanguages }.distinctUntilChanged(),
            ) { title, filter, onlyMine, languages ->
                filter.copy(title = title, availableLanguages = if (onlyMine) languages.toSet() else emptySet())
            }.distinctUntilChanged()
                .flatMapLatest { repository.search(it) }
                .cachedIn(viewModelScope)

        init {
            loadTags()
        }

        fun update(transform: (MangaFilter) -> MangaFilter) = _filter.update(transform)

        fun setOnlyMyLanguages(enabled: Boolean) {
            _onlyMyLanguages.value = enabled
        }

        fun setTitle(text: String) {
            val previous = title.value
            title.value = text
            // Relevance is the natural order while typing; without a title it means nothing.
            _filter.update {
                when {
                    text.isNotBlank() && previous.isBlank() && it.order == MangaOrder.Follows -> {
                        it.copy(
                            order = MangaOrder.Relevance
                        )
                    }

                    text.isBlank() && it.order == MangaOrder.Relevance -> {
                        it.copy(order = MangaOrder.Follows)
                    }

                    else -> {
                        it
                    }
                }
            }
        }

        /** Cycles a tag: neutral → included → excluded → neutral. */
        fun cycleTag(id: String) =
            _filter.update {
                when (id) {
                    in it.includedTags -> {
                        it.copy(
                            includedTags = it.includedTags - id,
                            excludedTags =
                                it.excludedTags + id
                        )
                    }

                    in it.excludedTags -> {
                        it.copy(excludedTags = it.excludedTags - id)
                    }

                    else -> {
                        it.copy(includedTags = it.includedTags + id)
                    }
                }
            }

        fun resetFilters() = _filter.update { MangaFilter(order = it.order) }

        fun loadTags() {
            viewModelScope.launch {
                _tags.value = TagsState.Loading
                _tags.value =
                    try {
                        TagsState.Loaded(repository.tags())
                    } catch (e: CancellationException) {
                        throw e
                    } catch (
                        @Suppress("TooGenericExceptionCaught") _: Exception,
                    ) {
                        TagsState.Failed
                    }
            }
        }

        @AssistedFactory
        interface Factory {
            fun create(args: SearchArgs): SearchViewModel
        }

        private companion object {
            const val TYPING_DEBOUNCE_MILLIS = 400L

            fun initialFilter(args: SearchArgs): MangaFilter {
                val section = args.section?.filter(emptyList())
                val order =
                    when {
                        section != null -> section.order
                        args.query.isBlank() -> MangaOrder.Follows
                        else -> MangaOrder.Relevance
                    }
                return (section ?: MangaFilter()).copy(order = order, includedTags = setOfNotNull(args.tagId))
            }
        }
    }
