package pt.aguiarvieira.m3mangadex.core.model

import java.time.Instant

enum class DownloadState { Queued, Downloading, Done, Failed }

/** A chapter saved (or being saved) on the device for offline reading. */
data class Download(
    val chapterId: String,
    val mangaId: String,
    val mangaTitle: String,
    val coverUrl: String?,
    val chapterNumber: String?,
    val chapterTitle: String?,
    val language: String,
    val state: DownloadState,
    val pagesDone: Int,
    val pageCount: Int,
    val sizeBytes: Long,
    val createdAt: Instant,
) {
    /** 0–1; 0 until the page count is known. */
    val progress: Float get() = if (pageCount > 0) pagesDone.toFloat() / pageCount else 0f

    /** Enough of the chapter to open it in the reader without a network. */
    fun toChapter(): Chapter =
        Chapter(
            id = chapterId,
            mangaId = mangaId,
            volume = null,
            number = chapterNumber,
            title = chapterTitle,
            language = language,
            pages = pageCount,
            externalUrl = null,
            readableAt = null,
            groups = emptyList(),
        )
}
