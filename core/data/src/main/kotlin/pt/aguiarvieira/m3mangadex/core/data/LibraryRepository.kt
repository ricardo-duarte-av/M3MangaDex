package pt.aguiarvieira.m3mangadex.core.data

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import pt.aguiarvieira.m3mangadex.core.model.FeedEntry
import pt.aguiarvieira.m3mangadex.core.model.Manga
import pt.aguiarvieira.m3mangadex.core.model.ReadingStatus

/** One manga in the user's MangaDex library. */
data class LibraryEntry(
    val manga: Manga,
    val status: ReadingStatus,
)

/** The logged-in user's MangaDex library, follows and read markers. Calls fail when logged out. */
interface LibraryRepository {
    /** Library statuses by manga id, as last fetched (empty when logged out). */
    val statuses: StateFlow<Map<String, ReadingStatus>>

    suspend fun refreshStatuses()

    /** Every manga in the library, with its status. */
    suspend fun library(): List<LibraryEntry>

    /** Files [mangaId] under [status] (following it too, as MangaDex's site does), or removes it (null). */
    suspend fun setStatus(
        mangaId: String,
        status: ReadingStatus?,
    )

    suspend fun isFollowing(mangaId: String): Boolean

    suspend fun setFollowing(
        mangaId: String,
        following: Boolean,
    )

    /** Ids of [mangaId]'s chapters read on MangaDex (on any device). */
    suspend fun readChapters(mangaId: String): Set<String>

    suspend fun setRead(
        mangaId: String,
        chapterIds: List<String>,
        read: Boolean,
    )

    /** New chapters of followed manga in the user's languages, newest first. */
    fun updates(): Flow<PagingData<FeedEntry>>

    /** Forget everything cached for the previous user. */
    fun clear()
}
