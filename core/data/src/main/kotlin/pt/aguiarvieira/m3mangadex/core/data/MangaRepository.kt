package pt.aguiarvieira.m3mangadex.core.data

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
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

    suspend fun manga(id: String): Manga

    suspend fun stats(id: String): MangaStats?

    /** Every chapter of [mangaId] in [languages], newest first. */
    suspend fun chapters(
        mangaId: String,
        languages: List<String>,
    ): List<Chapter>

    /** All tags, fetched once per process. */
    suspend fun tags(): List<Tag>
}
