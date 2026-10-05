package pt.aguiarvieira.m3mangadex.feature.updates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import pt.aguiarvieira.m3mangadex.core.auth.AuthRepository
import pt.aguiarvieira.m3mangadex.core.auth.Session
import pt.aguiarvieira.m3mangadex.core.data.LibraryRepository
import pt.aguiarvieira.m3mangadex.core.datastore.PreferencesDataSource
import pt.aguiarvieira.m3mangadex.core.model.FeedEntry
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class UpdatesViewModel
    @Inject
    constructor(
        auth: AuthRepository,
        library: LibraryRepository,
        preferences: PreferencesDataSource,
    ) : ViewModel() {
        val loggedIn: StateFlow<Boolean> =
            auth.session
                .map { it is Session.LoggedIn }
                .stateIn(viewModelScope, SharingStarted.Eagerly, auth.session.value is Session.LoggedIn)

        val languages: StateFlow<List<String>> =
            preferences.preferences.map { it.chapterLanguages }.stateIn(
                viewModelScope,
                SharingStarted.Eagerly,
                emptyList()
            )

        /** Restarts when the user logs in, or changes languages or ratings. */
        val updates: Flow<PagingData<FeedEntry>> =
            combine(
                loggedIn,
                preferences.preferences.map { it.chapterLanguages to it.contentRatings }
            ) { loggedIn, filters ->
                loggedIn to filters
            }.distinctUntilChanged()
                .flatMapLatest { (loggedIn, _) -> if (loggedIn) library.updates() else emptyFlow() }
                .cachedIn(viewModelScope)
    }
