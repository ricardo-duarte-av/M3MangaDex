package pt.aguiarvieira.m3mangadex.core.data.download

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** Where downloaded pages live: app-private storage, one folder per chapter, pages named by index. */
@Singleton
class DownloadStorage
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) {
        private val root = File(context.filesDir, "downloads")

        fun chapterDir(chapterId: String): File = File(root, chapterId)

        /** Page [index]'s file; [extension] from the page's original name ("png", "jpg"). */
        fun pageFile(
            chapterId: String,
            index: Int,
            extension: String,
        ): File = File(chapterDir(chapterId), "%03d.%s".format(index, extension))

        /** The chapter's saved pages in order, when there are exactly [count] of them. */
        fun pages(
            chapterId: String,
            count: Int,
        ): List<File>? {
            val files =
                chapterDir(chapterId)
                    .listFiles { file -> file.isFile && !file.name.endsWith(TEMP_SUFFIX) }
                    ?.sortedBy { it.name }
                    .orEmpty()
            return files.takeIf { it.size == count && count > 0 }
        }

        fun delete(chapterId: String) {
            chapterDir(chapterId).deleteRecursively()
        }

        fun sizeOf(chapterId: String): Long =
            chapterDir(chapterId).walkBottomUp().filter { it.isFile }.sumOf { it.length() }

        companion object {
            const val TEMP_SUFFIX = ".part"
        }
    }
