package pt.aguiarvieira.m3mangadex.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import pt.aguiarvieira.m3mangadex.core.model.ContentRating
import pt.aguiarvieira.m3mangadex.core.model.Languages
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
    ) {
        val preferences: Flow<UserPreferences> =
            dataStore.data.map { prefs ->
                UserPreferences(
                    // Ordered, so stored as one comma-separated string rather than a set.
                    chapterLanguages =
                        prefs[CHAPTER_LANGUAGES]?.split(',')?.filter(String::isNotBlank)?.takeIf { it.isNotEmpty() }
                            ?: Languages.defaults(Locale.getDefault()),
                    contentRatings =
                        prefs[CONTENT_RATINGS]
                            ?.mapNotNull { value -> ContentRating.entries.firstOrNull { it.apiValue == value } }
                            ?.toSet()
                            ?.takeIf { it.isNotEmpty() }
                            ?: ContentRating.Default,
                    dataSaver = prefs[DATA_SAVER] ?: false,
                )
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
        }
    }
