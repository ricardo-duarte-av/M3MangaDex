package pt.aguiarvieira.m3mangadex.core.data.download

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import pt.aguiarvieira.m3mangadex.core.database.DownloadDao
import pt.aguiarvieira.m3mangadex.core.database.DownloadEntity
import pt.aguiarvieira.m3mangadex.core.model.Chapter
import pt.aguiarvieira.m3mangadex.core.model.Covers
import pt.aguiarvieira.m3mangadex.core.model.Download
import pt.aguiarvieira.m3mangadex.core.model.DownloadState
import pt.aguiarvieira.m3mangadex.core.model.Manga
import pt.aguiarvieira.m3mangadex.core.model.coverUrl
import java.io.File
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DefaultDownloadRepository
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val dao: DownloadDao,
        private val storage: DownloadStorage,
    ) : DownloadRepository {
        override val downloads: Flow<List<Download>> = dao.all().map { rows -> rows.map { it.toModel() } }

        override fun forManga(mangaId: String): Flow<Map<String, Download>> =
            dao.forManga(mangaId).map { rows -> rows.associate { it.chapterId to it.toModel() } }

        override suspend fun download(chapterId: String): Download? = dao.get(chapterId)?.toModel()

        override suspend fun enqueue(
            manga: Manga,
            chapters: List<Chapter>,
        ) {
            val now = System.currentTimeMillis()
            val title = manga.displayTitle(listOf("en"))
            val rows =
                chapters.filterNot { it.isExternal }.mapIndexed { index, chapter ->
                    DownloadEntity(
                        chapterId = chapter.id,
                        mangaId = manga.id,
                        mangaTitle = title,
                        coverUrl = manga.coverUrl(Covers.Size.Thumbnail),
                        chapterNumber = chapter.number,
                        chapterTitle = chapter.title,
                        language = chapter.language,
                        state = DownloadState.Queued.name,
                        pagesDone = 0,
                        pageCount = chapter.pages,
                        sizeBytes = 0,
                        // Keeps the order chapters were asked for.
                        createdAt = now + index,
                    )
                }
            if (rows.isEmpty()) return
            dao.insert(rows)
            DownloadWorker.start(context)
        }

        override suspend fun retry(chapterId: String) {
            val row = dao.get(chapterId) ?: return
            dao.upsert(row.copy(state = DownloadState.Queued.name, createdAt = System.currentTimeMillis()))
            DownloadWorker.start(context)
        }

        override suspend fun delete(chapterId: String) {
            // Row first: the downloader checks it between pages and stops when it's gone.
            dao.delete(chapterId)
            withContext(Dispatchers.IO) { storage.delete(chapterId) }
        }

        override suspend fun deleteManga(mangaId: String) {
            dao.forMangaNow(mangaId).forEach { delete(it.chapterId) }
        }

        override suspend fun pages(chapterId: String): List<File>? {
            val row = dao.get(chapterId)?.takeIf { it.state == DownloadState.Done.name } ?: return null
            return withContext(Dispatchers.IO) { storage.pages(chapterId, row.pageCount) }
        }
    }

internal fun DownloadEntity.toModel() =
    Download(
        chapterId = chapterId,
        mangaId = mangaId,
        mangaTitle = mangaTitle,
        coverUrl = coverUrl,
        chapterNumber = chapterNumber,
        chapterTitle = chapterTitle,
        language = language,
        state = DownloadState.entries.firstOrNull { it.name == state } ?: DownloadState.Failed,
        pagesDone = pagesDone,
        pageCount = pageCount,
        sizeBytes = sizeBytes,
        createdAt = Instant.ofEpochMilli(createdAt),
    )
