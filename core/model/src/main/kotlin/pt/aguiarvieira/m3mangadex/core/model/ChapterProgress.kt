package pt.aguiarvieira.m3mangadex.core.model

import java.time.Instant

/** How far into a chapter the user got, kept on the device (synced with MangaDex from M4). */
data class ChapterProgress(
    val chapterId: String,
    val mangaId: String,
    /** The chapter's number as shown ("12", "12.5"), for "Continue Ch. 12" without a lookup. */
    val chapterNumber: String?,
    /** Zero-based page last shown. */
    val page: Int,
    val pageCount: Int,
    val readAt: Instant,
) {
    /** Reaching the last page counts as having read the chapter. */
    val isRead: Boolean get() = pageCount > 0 && page >= pageCount - 1
}
