package pt.aguiarvieira.m3mangadex.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pt.aguiarvieira.m3mangadex.core.auth.AuthRepository
import pt.aguiarvieira.m3mangadex.core.auth.Session
import pt.aguiarvieira.m3mangadex.core.data.LibraryEntry
import pt.aguiarvieira.m3mangadex.core.data.LibraryRepository
import pt.aguiarvieira.m3mangadex.core.datastore.PreferencesDataSource
import pt.aguiarvieira.m3mangadex.core.model.ReadingStatus
import javax.inject.Inject

sealed interface LibraryUiState {
    data object LoggedOut : LibraryUiState

    data object Loading : LibraryUiState

    data object Failed : LibraryUiState

    data class Loaded(
        val entries: List<LibraryEntry>,
        val filter: ReadingStatus? = null,
        val refreshing: Boolean = false,
    ) : LibraryUiState {
        val shown: List<LibraryEntry> get() = entries.filter { filter == null || it.status == filter }

        fun count(status: ReadingStatus) = entries.count { it.status == status }
    }
}

@HiltViewModel
class LibraryViewModel
    @Inject
    constructor(
        auth: AuthRepository,
        private val library: LibraryRepository,
        preferences: PreferencesDataSource,
    ) : ViewModel() {
        private val _state = MutableStateFlow<LibraryUiState>(LibraryUiState.Loading)
        val state: StateFlow<LibraryUiState> = _state.asStateFlow()

        /** For picking titles in the user's languages. */
        val languages =
            preferences.preferences.map { it.chapterLanguages }

        init {
            viewModelScope.launch {
                auth.session.map { it is Session.LoggedIn }.distinctUntilChanged().collect { loggedIn ->
                    if (loggedIn) load(initial = true) else _state.value = LibraryUiState.LoggedOut
                }
            }
            // Statuses changed elsewhere (a details screen): reflect them without a refetch.
            viewModelScope.launch {
                library.statuses.collect { statuses ->
                    _state.update { state ->
                        if (state !is LibraryUiState.Loaded) return@update state
                        state.copy(
                            entries =
                                state.entries.mapNotNull { e ->
                                    statuses[e.manga.id]?.let { e.copy(status = it) }
                                }
                        )
                    }
                }
            }
        }

        fun setFilter(status: ReadingStatus?) =
            _state.update {
                (it as? LibraryUiState.Loaded)?.copy(filter = status)
                    ?: it
            }

        fun refresh() {
            viewModelScope.launch { load(initial = false) }
        }

        private suspend fun load(initial: Boolean) {
            val current = _state.value as? LibraryUiState.Loaded
            if (initial && current == null) _state.value = LibraryUiState.Loading
            _state.update { (it as? LibraryUiState.Loaded)?.copy(refreshing = true) ?: it }
            _state.value =
                try {
                    val entries =
                        library.library().sortedBy {
                            it.manga.title.values
                                .firstOrNull()
                                .orEmpty()
                                .lowercase()
                        }
                    LibraryUiState.Loaded(entries, filter = current?.filter)
                } catch (e: CancellationException) {
                    throw e
                } catch (
                    @Suppress("TooGenericExceptionCaught") _: Exception,
                ) {
                    current?.copy(refreshing = false) ?: LibraryUiState.Failed
                }
        }
    }
