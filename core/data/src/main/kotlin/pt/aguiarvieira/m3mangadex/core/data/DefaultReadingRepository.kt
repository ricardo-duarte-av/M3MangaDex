package pt.aguiarvieira.m3mangadex.core.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import pt.aguiarvieira.m3mangadex.core.database.ChapterProgressEntity
import pt.aguiarvieira.m3mangadex.core.database.MangaSettingsEntity
import pt.aguiarvieira.m3mangadex.core.database.ReadingDao
import pt.aguiarvieira.m3mangadex.core.model.Chapter
import pt.aguiarvieira.m3mangadex.core.model.ChapterProgress
import pt.aguiarvieira.m3mangadex.core.model.ReaderMode
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DefaultReadingRepository
    @Inject
    constructor(
        private val dao: ReadingDao,
    ) : ReadingRepository {
        override fun progress(mangaId: String): Flow<Map<String, ChapterProgress>> =
            dao.progress(mangaId).map { rows -> rows.associate { it.chapterId to it.toModel() } }.distinctUntilChanged()

        override fun lastRead(mangaId: String): Flow<ChapterProgress?> =
            dao
                .lastRead(mangaId)
                .map {
                    it?.toModel()
                }.distinctUntilChanged()

        override suspend fun progressOf(chapterId: String): ChapterProgress? = dao.progressOf(chapterId)?.toModel()

        override suspend fun saveProgress(
            mangaId: String,
            chapter: Chapter,
            page: Int,
            pageCount: Int,
        ) {
            // Never move backwards past "read": paging back through a finished chapter keeps it read.
            val previous = dao.progressOf(chapter.id)
            val wasRead = previous != null && previous.pageCount > 0 && previous.page >= previous.pageCount - 1
            dao.upsertProgress(
                ChapterProgressEntity(
                    chapterId = chapter.id,
                    mangaId = mangaId,
                    chapterNumber = chapter.number,
                    page = if (wasRead) maxOf(previous.page, page) else page,
                    pageCount = pageCount,
                    readAt = System.currentTimeMillis(),
                ),
            )
        }

        override suspend fun clearProgress(chapterId: String) = dao.deleteProgress(chapterId)

        override fun readerMode(mangaId: String): Flow<ReaderMode?> =
            dao
                .readerMode(mangaId)
                .map { name ->
                    ReaderMode.entries.firstOrNull { it.name == name }
                }.distinctUntilChanged()

        override suspend fun setReaderMode(
            mangaId: String,
            mode: ReaderMode,
        ) = updateSettings(mangaId) { it.copy(readerMode = mode.name) }

        override fun extraLanguages(mangaId: String): Flow<List<String>> =
            dao
                .settings(mangaId)
                .map {
                    it
                        ?.extraLanguages
                        ?.split(',')
                        ?.filter(String::isNotBlank)
                        .orEmpty()
                }.distinctUntilChanged()

        override suspend fun setExtraLanguages(
            mangaId: String,
            languages: List<String>,
        ) = updateSettings(mangaId) { it.copy(extraLanguages = languages.joinToString(",").ifEmpty { null }) }

        /** Changes one setting of a manga without clobbering the others in its row. */
        private suspend fun updateSettings(
            mangaId: String,
            change: (MangaSettingsEntity) -> MangaSettingsEntity,
        ) = settingsLock.withLock {
            val current = dao.settingsNow(mangaId) ?: MangaSettingsEntity(mangaId, readerMode = null)
            dao.upsertSettings(change(current))
        }

        private val settingsLock = Mutex()
    }

private fun ChapterProgressEntity.toModel() =
    ChapterProgress(chapterId, mangaId, chapterNumber, page, pageCount, Instant.ofEpochMilli(readAt))
