package pt.aguiarvieira.m3mangadex.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import pt.aguiarvieira.m3mangadex.core.model.ContentPolicy
import pt.aguiarvieira.m3mangadex.core.model.ContentRating
import pt.aguiarvieira.m3mangadex.core.model.Languages
import pt.aguiarvieira.m3mangadex.core.model.PageFit
import pt.aguiarvieira.m3mangadex.core.model.UserPreferences
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/** The user's reading preferences, persisted with Preferences DataStore. */
@Singleton
class PreferencesDataSource
    @Inject
    constructor(
        private val dataStore: DataStore<Preferences>,
        private val policy: ContentPolicy,
    ) {
        val preferences: Flow<UserPreferences> =
            dataStore.data.map { prefs ->
                UserPreferences(
                    // Ordered, so stored as one comma-separated string rather than a set.
                    chapterLanguages =
                        prefs[CHAPTER_LANGUAGES]?.split(',')?.filter(String::isNotBlank)?.takeIf { it.isNotEmpty() }
                            ?: Languages.defaults(Locale.getDefault()),
                    // Limited to this build's policy, whatever was stored (say, by a sideloaded build).
                    contentRatings =
                        policy.allowed(
                            prefs[CONTENT_RATINGS]
                                ?.mapNotNull { value -> ContentRating.entries.firstOrNull { it.apiValue == value } }
                                ?.toSet()
                                .orEmpty(),
                        ),
                    dataSaver = prefs[DATA_SAVER] ?: false,
                    volumeKeyPaging = prefs[VOLUME_KEY_PAGING] ?: false,
                    doublePageSpreads = prefs[DOUBLE_PAGE_SPREADS] ?: true,
                    pageFit = PageFit.entries.firstOrNull { it.name == prefs[PAGE_FIT] } ?: PageFit.Auto,
                    cropBorders = prefs[CROP_BORDERS] ?: true,
                    coverTheming = prefs[COVER_THEMING] ?: true,
                    newChapterNotifications = prefs[NEW_CHAPTER_NOTIFICATIONS] ?: false,
                )
            }

        /**
         * The latest preferences, readable without suspending (null only in the first instant after
         * process start): for first frames that need them, like a details header's title.
         */
        val current: StateFlow<UserPreferences?> =
            preferences.stateIn(CoroutineScope(SupervisorJob() + Dispatchers.Default), SharingStarted.Eagerly, null)

        /** Enables or disables new-chapter checks; enabling starts counting from now (no backlog flood). */
        suspend fun setNewChapterNotifications(enabled: Boolean) {
            dataStore.edit {
                it[NEW_CHAPTER_NOTIFICATIONS] = enabled
                if (enabled) it[LAST_CHAPTER_CHECK] = System.currentTimeMillis()
            }
        }

        /** Epoch millis up to which new chapters have been notified about (0: never checked). */
        val lastChapterCheck: Flow<Long> = dataStore.data.map { it[LAST_CHAPTER_CHECK] ?: 0L }

        suspend fun setLastChapterCheck(millis: Long) {
            dataStore.edit { it[LAST_CHAPTER_CHECK] = millis }
        }

        suspend fun setPageFit(fit: PageFit) {
            dataStore.edit { it[PAGE_FIT] = fit.name }
        }

        suspend fun setCropBorders(enabled: Boolean) {
            dataStore.edit { it[CROP_BORDERS] = enabled }
        }

        suspend fun setCoverTheming(enabled: Boolean) {
            dataStore.edit { it[COVER_THEMING] = enabled }
        }

        suspend fun setVolumeKeyPaging(enabled: Boolean) {
            dataStore.edit { it[VOLUME_KEY_PAGING] = enabled }
        }

        suspend fun setDoublePageSpreads(enabled: Boolean) {
            dataStore.edit { it[DOUBLE_PAGE_SPREADS] = enabled }
        }

        suspend fun setChapterLanguages(languages: List<String>) {
            dataStore.edit { it[CHAPTER_LANGUAGES] = languages.joinToString(",") }
        }

        suspend fun setContentRatings(ratings: Set<ContentRating>) {
            dataStore.edit { it[CONTENT_RATINGS] = ratings.map(ContentRating::apiValue).toSet() }
        }

        suspend fun setDataSaver(enabled: Boolean) {
            dataStore.edit { it[DATA_SAVER] = enabled }
        }

        private companion object {
            val CHAPTER_LANGUAGES = stringPreferencesKey("chapter_languages")
            val CONTENT_RATINGS = stringSetPreferencesKey("content_ratings")
            val DATA_SAVER = booleanPreferencesKey("data_saver")
            val VOLUME_KEY_PAGING = booleanPreferencesKey("volume_key_paging")
            val DOUBLE_PAGE_SPREADS = booleanPreferencesKey("double_page_spreads")
            val PAGE_FIT = stringPreferencesKey("page_fit")
            val CROP_BORDERS = booleanPreferencesKey("crop_borders")
            val COVER_THEMING = booleanPreferencesKey("cover_theming")
            val NEW_CHAPTER_NOTIFICATIONS = booleanPreferencesKey("new_chapter_notifications")
            val LAST_CHAPTER_CHECK = longPreferencesKey("last_chapter_check")
        }
    }
