package pt.aguiarvieira.m3mangadex.core.database

import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReadingDaoTest {
    private val db = M3MangaDexDatabase.build(ApplicationProvider.getApplicationContext(), name = null, driver = AndroidSQLiteDriver())
    private val dao = db.readingDao()

    @After
    fun tearDown() = db.close()

    private fun progress(
        chapter: String,
        readAt: Long,
        page: Int = 0,
    ) = ChapterProgressEntity(chapter, "m", chapter, page, 20, readAt)

    @Test
    fun `last read is the most recent chapter, and re-reading updates in place`() =
        runTest {
            dao.upsertProgress(progress("1", readAt = 1))
            dao.upsertProgress(progress("2", readAt = 2))
            assertEquals("2", dao.lastRead("m").first()?.chapterId)

            dao.upsertProgress(progress("1", readAt = 3, page = 19))
            assertEquals("1", dao.lastRead("m").first()?.chapterId)
            assertEquals(2, dao.progress("m").first().size)
            assertEquals(19, dao.progressOf("1")?.page)
        }

    @Test
    fun `reader mode is null until chosen`() =
        runTest {
            assertNull(dao.readerMode("m").first())
            dao.upsertSettings(MangaSettingsEntity("m", "Webtoon"))
            assertEquals("Webtoon", dao.readerMode("m").first())
        }
}
