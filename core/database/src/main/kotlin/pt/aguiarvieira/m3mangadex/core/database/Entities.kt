package pt.aguiarvieira.m3mangadex.core.database

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

/** The last page shown of a chapter the user opened. */
@Entity(tableName = "chapter_progress", indices = [Index("mangaId", "readAt")])
data class ChapterProgressEntity(
    @PrimaryKey val chapterId: String,
    val mangaId: String,
    val chapterNumber: String?,
    val page: Int,
    val pageCount: Int,
    /** Epoch millis. */
    val readAt: Long,
)

/** Per-manga reader choices; a missing row means "use the defaults". */
@Entity(tableName = "manga_settings")
data class MangaSettingsEntity(
    @PrimaryKey val mangaId: String,
    /** A `ReaderMode` name, or null for the default for that manga. */
    val readerMode: String?,
)
