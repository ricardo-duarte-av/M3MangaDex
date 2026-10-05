package pt.aguiarvieira.m3mangadex.core.data.download

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import pt.aguiarvieira.m3mangadex.core.database.DownloadDao
import pt.aguiarvieira.m3mangadex.core.database.DownloadEntity
import pt.aguiarvieira.m3mangadex.core.datastore.PreferencesDataSource
import pt.aguiarvieira.m3mangadex.core.model.AtHomeServer
import pt.aguiarvieira.m3mangadex.core.model.Download
import pt.aguiarvieira.m3mangadex.core.model.DownloadState
import pt.aguiarvieira.m3mangadex.core.network.MangaDexApi
import pt.aguiarvieira.m3mangadex.core.network.di.ImageClient
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Works through the download queue, one chapter at a time, page by page. Pages come from
 * MangaDex@Home through the image client (so every page is reported, as for reading online); a
 * failing page gets a fresh server once before the chapter is marked failed. Already-saved pages
 * are kept, so an interrupted chapter resumes where it stopped.
 */
@Singleton
internal class DownloadEngine
    @Inject
    constructor(
        private val dao: DownloadDao,
        private val api: MangaDexApi,
        @param:ImageClient private val images: OkHttpClient,
        private val storage: DownloadStorage,
        private val preferences: PreferencesDataSource,
    ) {
        /** Runs until the queue is empty; [onProgress] after every page. */
        suspend fun run(onProgress: suspend (Download) -> Unit) {
            while (true) {
                val next = dao.next() ?: return
                download(next, onProgress)
            }
        }

        private suspend fun download(
            row: DownloadEntity,
            onProgress: suspend (Download) -> Unit,
        ) {
            val id = row.chapterId
            val dataSaver = preferences.preferences.first().dataSaver
            var server = attempt { api.atHomeServer(id) } ?: return fail(row)
            val count = server.pageCount
            if (count == 0) return fail(row)
            var done = 0
            for (index in 0 until count) {
                // Deleted (cancelled) from the UI: stop, and leave nothing behind.
                if (dao.get(id) == null) {
                    withContext(Dispatchers.IO) { storage.delete(id) }
                    return
                }
                val ok =
                    savePage(server, id, index, dataSaver) ||
                        // The node may be gone or expired: ask for another, once per page.
                        (
                            attempt {
                                api.atHomeServer(
                                    id
                                )
                            }?.also { server = it }?.let { savePage(it, id, index, dataSaver) } ==
                                true
                        )
                if (!ok) return fail(row.copy(pagesDone = done, pageCount = count))
                done++
                dao.update(id, DownloadState.Downloading.name, done, count, 0)
                onProgress(
                    row.copy(state = DownloadState.Downloading.name, pagesDone = done, pageCount = count).toModel()
                )
            }
            if (dao.get(id) == null) return
            val size = withContext(Dispatchers.IO) { storage.sizeOf(id) }
            dao.update(id, DownloadState.Done.name, count, count, size)
        }

        /** Saves one page (unless it already is); false when it couldn't be fetched. */
        private suspend fun savePage(
            server: AtHomeServer,
            chapterId: String,
            index: Int,
            dataSaver: Boolean,
        ): Boolean =
            withContext(Dispatchers.IO) {
                val url = server.pageUrl(index, dataSaver)
                val file = storage.pageFile(chapterId, index, url.substringAfterLast('.', "jpg"))
                if (file.isFile && file.length() > 0) return@withContext true
                file.parentFile?.mkdirs()
                val temp = File(file.path + DownloadStorage.TEMP_SUFFIX)
                try {
                    images.newCall(Request.Builder().url(url).build()).execute().use { response ->
                        if (!response.isSuccessful) return@withContext false
                        temp.outputStream().use { out -> response.body.byteStream().copyTo(out) }
                    }
                    temp.renameTo(file)
                } catch (_: IOException) {
                    temp.delete()
                    false
                }
            }

        private suspend fun fail(row: DownloadEntity) {
            if (dao.get(row.chapterId) == null) return
            dao.update(row.chapterId, DownloadState.Failed.name, row.pagesDone, row.pageCount, 0)
        }

        private suspend fun <T> attempt(block: suspend () -> T): T? =
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (
                @Suppress("TooGenericExceptionCaught") _: Exception,
            ) {
                null
            }
    }
