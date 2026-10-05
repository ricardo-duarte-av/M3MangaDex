package pt.aguiarvieira.m3mangadex.core.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import pt.aguiarvieira.m3mangadex.core.datastore.PreferencesDataSource
import javax.inject.Inject

/**
 * The chapter languages for one manga: the user's global choice, then any extra languages picked
 * for that manga (say, Spanish for a series not translated into the user's own languages). The
 * details screen and the reader both list chapters through this, so their neighbours agree.
 */
class ChapterLanguages
    @Inject
    constructor(
        private val preferences: PreferencesDataSource,
        private val reading: ReadingRepository,
    ) {
        fun forManga(mangaId: String): Flow<List<String>> =
            combine(preferences.preferences, reading.extraLanguages(mangaId)) { prefs, extra ->
                (prefs.chapterLanguages + extra).distinct()
            }.distinctUntilChanged()
    }
