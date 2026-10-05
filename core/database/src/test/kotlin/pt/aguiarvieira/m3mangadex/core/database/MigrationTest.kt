package pt.aguiarvieira.m3mangadex.core.database

import android.content.Context
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.sqlite.execSQL
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    /** A database exactly as v0.2.0 shipped it (schema version 1), with one choice in it. */
    private fun createVersion1(name: String) {
        val path = context.getDatabasePath(name).apply { parentFile?.mkdirs() }.absolutePath
        AndroidSQLiteDriver().open(path).run {
            execSQL(
                "CREATE TABLE IF NOT EXISTS `chapter_progress` (`chapterId` TEXT NOT NULL, `mangaId` TEXT NOT NULL, " +
                    "`chapterNumber` TEXT, `page` INTEGER NOT NULL, `pageCount` INTEGER NOT NULL, `readAt` INTEGER NOT NULL, " +
                    "PRIMARY KEY(`chapterId`))",
            )
            execSQL("CREATE INDEX IF NOT EXISTS `index_chapter_progress_mangaId_readAt` ON `chapter_progress` (`mangaId`, `readAt`)")
            execSQL("CREATE TABLE IF NOT EXISTS `manga_settings` (`mangaId` TEXT NOT NULL, `readerMode` TEXT, PRIMARY KEY(`mangaId`))")
            execSQL("INSERT INTO manga_settings (mangaId, readerMode) VALUES ('m', 'Webtoon')")
            execSQL("INSERT INTO chapter_progress VALUES ('c1', 'm', '1', 4, 24, 1000)")
            execSQL("PRAGMA user_version = 1")
            close()
        }
    }

    @Test
    fun `version 1 data survives the upgrade to the current schema`() =
        runTest {
            val name = "migration-test.db"
            context.deleteDatabase(name)
            createVersion1(name)

            val db = M3MangaDexDatabase.build(context, name = name, driver = AndroidSQLiteDriver())
            val dao = db.readingDao()
            assertEquals("Webtoon", dao.readerMode("m").first())
            assertNull(dao.settingsNow("m")?.extraLanguages)
            assertEquals(4, dao.progressOf("c1")?.page)
            // v3's downloads table exists and works.
            val downloads = db.downloadDao()
            downloads.insert(listOf(DownloadEntity("c9", "m", "Title", null, "1", null, "en", "Queued", 0, 0, 0, 1)))
            assertEquals("c9", downloads.next()?.chapterId)
            db.close()
        }
}
