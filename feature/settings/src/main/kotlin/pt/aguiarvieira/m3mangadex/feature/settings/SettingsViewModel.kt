package pt.aguiarvieira.m3mangadex.feature.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pt.aguiarvieira.m3mangadex.core.auth.AuthRepository
import pt.aguiarvieira.m3mangadex.core.auth.Session
import pt.aguiarvieira.m3mangadex.core.data.LibraryRepository
import pt.aguiarvieira.m3mangadex.core.data.notify.NewChaptersWorker
import pt.aguiarvieira.m3mangadex.core.datastore.PreferencesDataSource
import pt.aguiarvieira.m3mangadex.core.model.ContentPolicy
import pt.aguiarvieira.m3mangadex.core.model.ContentRating
import pt.aguiarvieira.m3mangadex.core.model.UserPreferences
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val preferences: PreferencesDataSource,
        private val auth: AuthRepository,
        private val library: LibraryRepository,
        @param:ApplicationContext private val context: Context,
        policy: ContentPolicy,
    ) : ViewModel() {
        /** The ratings this build lets the user choose from. */
        val selectableRatings: List<ContentRating> = policy.selectable

        fun setNewChapterNotifications(enabled: Boolean) {
            viewModelScope.launch { preferences.setNewChapterNotifications(enabled) }
        }

        /** Debug builds: run a check now, looking back two days, to see notifications without waiting. */
        fun checkNewChaptersNow() =
            NewChaptersWorker.checkNow(
                context,
                System.currentTimeMillis() - TEST_LOOKBACK_MILLIS
            )

        val session: StateFlow<Session> = auth.session

        fun logout() {
            auth.logout()
            library.clear()
        }

        val state: StateFlow<UserPreferences?> =
            preferences.preferences.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

        /** Adds [code] at the end (lowest priority) or removes it; the last language can't go. */
        fun toggleLanguage(code: String) {
            val current = state.value?.chapterLanguages ?: return
            val next = if (code in current) current - code else current + code
            if (next.isNotEmpty()) viewModelScope.launch { preferences.setChapterLanguages(next) }
        }

        fun toggleRating(rating: ContentRating) {
            val current = state.value?.contentRatings ?: return
            val next = if (rating in current) current - rating else current + rating
            if (next.isNotEmpty()) viewModelScope.launch { preferences.setContentRatings(next) }
        }

        fun setDataSaver(enabled: Boolean) {
            viewModelScope.launch { preferences.setDataSaver(enabled) }
        }

        fun setDoublePageSpreads(enabled: Boolean) {
            viewModelScope.launch { preferences.setDoublePageSpreads(enabled) }
        }

        fun setCoverTheming(enabled: Boolean) {
            viewModelScope.launch { preferences.setCoverTheming(enabled) }
        }

        fun setCropBorders(enabled: Boolean) {
            viewModelScope.launch { preferences.setCropBorders(enabled) }
        }

        fun setVolumeKeyPaging(enabled: Boolean) {
            viewModelScope.launch { preferences.setVolumeKeyPaging(enabled) }
        }

        private companion object {
            const val STOP_TIMEOUT_MILLIS = 5_000L
            const val TEST_LOOKBACK_MILLIS = 48 * 60 * 60 * 1000L
        }
    }
