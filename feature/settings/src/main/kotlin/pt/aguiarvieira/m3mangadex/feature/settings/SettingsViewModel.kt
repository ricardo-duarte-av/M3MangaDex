package pt.aguiarvieira.m3mangadex.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pt.aguiarvieira.m3mangadex.core.datastore.PreferencesDataSource
import pt.aguiarvieira.m3mangadex.core.model.ContentRating
import pt.aguiarvieira.m3mangadex.core.model.UserPreferences
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val preferences: PreferencesDataSource,
    ) : ViewModel() {
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

        fun setVolumeKeyPaging(enabled: Boolean) {
            viewModelScope.launch { preferences.setVolumeKeyPaging(enabled) }
        }

        private companion object {
            const val STOP_TIMEOUT_MILLIS = 5_000L
        }
    }
