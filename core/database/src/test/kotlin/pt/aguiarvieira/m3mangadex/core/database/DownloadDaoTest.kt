package pt.aguiarvieira.m3mangadex.core.database

import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DownloadDaoTest {
    private val db = M3MangaDexDatabase.build(ApplicationProvider.getApplicationContext(), name = null, driver = AndroidSQLiteDriver())
    private val dao = db.downloadDao()

    @After
    fun tearDown() = db.close()

    private fun download(
        id: String,
        state: String = "Queued",
        createdAt: Long,
    ) = DownloadEntity(id, "m", "Title", null, id, null, "en", state, 0, 0, 0, createdAt)

    @Test
    fun `next resumes an interrupted download before newer queued ones`() =
        runTest {
            dao.insert(listOf(download("a", createdAt = 1), download("b", createdAt = 2), download("c", "Done", 0)))
            assertEquals("a", dao.next()?.chapterId)
            dao.update("b", "Downloading", 3, 20, 100)
            assertEquals("b", dao.next()?.chapterId)
            dao.update("b", "Done", 20, 20, 1000)
            dao.delete("a")
            assertNull(dao.next())
        }

    @Test
    fun `re-queueing a known chapter leaves it alone`() =
        runTest {
            dao.insert(listOf(download("a", "Done", 1)))
            dao.insert(listOf(download("a", createdAt = 5)))
            assertEquals("Done", dao.get("a")?.state)
        }
}
