package pt.aguiarvieira.m3mangadex.core.data

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import pt.aguiarvieira.m3mangadex.core.model.AtHomeServer
import pt.aguiarvieira.m3mangadex.core.model.Chapter
import pt.aguiarvieira.m3mangadex.core.model.Manga
import pt.aguiarvieira.m3mangadex.core.model.MangaFilter
import pt.aguiarvieira.m3mangadex.core.model.MangaStats
import pt.aguiarvieira.m3mangadex.core.model.Tag

interface MangaRepository {
    /** One Browse row, in the user's languages and content ratings. */
    suspend fun section(
        section: BrowseSection,
        limit: Int,
    ): List<Manga>

    /** Paged search; an empty content-rating set in [filter] means the user's preference. */
    fun search(filter: MangaFilter): Flow<PagingData<Manga>>

    /** A handful of title matches, for search suggestions. */
    suspend fun suggestions(
        title: String,
        limit: Int,
    ): List<Manga>

    /**
     * A manga as a list last showed it (possibly without authors), or null: lets the details screen
     * draw its header on the very first frame, so the tapped cover has somewhere to fly to.
     */
    fun preview(id: String): Manga?

    /** A manga, from a short-lived memory cache when the details screen or reader just loaded it. */
    suspend fun manga(id: String): Manga

    suspend fun stats(id: String): MangaStats?

    /** Every chapter of [mangaId] in [languages], newest first; cached briefly like [manga]. */
    suspend fun chapters(
        mangaId: String,
        languages: List<String>,
    ): List<Chapter>

    suspend fun chapter(id: String): Chapter

    /** Where [chapterId]'s pages can be fetched from for the next ~15 minutes. Never cached. */
    suspend fun atHomeServer(chapterId: String): AtHomeServer

    /** All tags, fetched once per process. */
    suspend fun tags(): List<Tag>
}
