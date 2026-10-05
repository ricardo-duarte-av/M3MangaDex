package pt.aguiarvieira.m3mangadex.core.data

import kotlinx.coroutines.flow.Flow
import pt.aguiarvieira.m3mangadex.core.model.Chapter
import pt.aguiarvieira.m3mangadex.core.model.ChapterProgress
import pt.aguiarvieira.m3mangadex.core.model.ReaderMode

/** Local reading state: progress per chapter and the reader mode chosen per manga. */
interface ReadingRepository {
    /** Every chapter of [mangaId] the user has opened, by chapter id. */
    fun progress(mangaId: String): Flow<Map<String, ChapterProgress>>

    /** The chapter of [mangaId] read most recently, for "Continue reading". */
    fun lastRead(mangaId: String): Flow<ChapterProgress?>

    suspend fun progressOf(chapterId: String): ChapterProgress?

    suspend fun saveProgress(
        mangaId: String,
        chapter: Chapter,
        page: Int,
        pageCount: Int,
    )

    /** Forgets [chapterId]'s progress: it reads as unread again. */
    suspend fun clearProgress(chapterId: String)

    /** The mode the user picked for [mangaId], or null to use [ReaderMode.defaultFor]. */
    fun readerMode(mangaId: String): Flow<ReaderMode?>

    suspend fun setReaderMode(
        mangaId: String,
        mode: ReaderMode,
    )

    /** Chapter languages shown for [mangaId] on top of the global ones. */
    fun extraLanguages(mangaId: String): Flow<List<String>>

    suspend fun setExtraLanguages(
        mangaId: String,
        languages: List<String>,
    )
}
