package pt.aguiarvieira.m3mangadex.core.data.download

import kotlinx.coroutines.flow.Flow
import pt.aguiarvieira.m3mangadex.core.model.Chapter
import pt.aguiarvieira.m3mangadex.core.model.Download
import pt.aguiarvieira.m3mangadex.core.model.Manga
import java.io.File

/** Chapters saved for offline reading. Downloads run in the background, one chapter at a time. */
interface DownloadRepository {
    val downloads: Flow<List<Download>>

    /** [mangaId]'s downloads, by chapter id. */
    fun forManga(mangaId: String): Flow<Map<String, Download>>

    suspend fun download(chapterId: String): Download?

    /** Queues [chapters] of [manga] (publisher-hosted ones are skipped: nothing to save). */
    suspend fun enqueue(
        manga: Manga,
        chapters: List<Chapter>,
    )

    suspend fun retry(chapterId: String)

    /** Cancels (if queued or running) and deletes a download and its files. */
    suspend fun delete(chapterId: String)

    suspend fun deleteManga(mangaId: String)

    /** A finished download's page files, in order; null when it isn't (fully) on the device. */
    suspend fun pages(chapterId: String): List<File>?
}
