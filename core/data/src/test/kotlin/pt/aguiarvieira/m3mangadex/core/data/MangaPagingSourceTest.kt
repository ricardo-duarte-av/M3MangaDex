package pt.aguiarvieira.m3mangadex.core.data

import androidx.paging.PagingSource
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pt.aguiarvieira.m3mangadex.core.model.Manga
import pt.aguiarvieira.m3mangadex.core.model.Page
import java.io.IOException

class MangaPagingSourceTest {
    private val requests = mutableListOf<Pair<Int, Int>>()

    private fun source(total: Int) =
        MangaPagingSource { offset, limit ->
            requests += offset to limit
            Page(List(minOf(limit, total - offset)) { manga("$offset-$it") }, offset, total)
        }

    private fun refresh(size: Int) = PagingSource.LoadParams.Refresh<Int>(null, size, false)

    private fun append(
        key: Int,
        size: Int,
    ) = PagingSource.LoadParams.Append(key, size, false)

    @Test
    fun `pages by offset until the total`() =
        runTest {
            val source = source(total = 45)
            val first = source.load(refresh(30)) as PagingSource.LoadResult.Page
            assertEquals(30, first.nextKey)
            val second = source.load(append(30, 30)) as PagingSource.LoadResult.Page
            assertEquals(15, second.data.size)
            assertNull(second.nextKey)
        }

    @Test
    fun `never asks past the 10000-result window`() =
        runTest {
            val source = source(total = 50_000)
            val page = source.load(append(9_990, 30)) as PagingSource.LoadResult.Page
            assertEquals(9_990 to 10, requests.single())
            assertNull(page.nextKey)
        }

    @Test
    fun `errors become LoadResult Error`() =
        runTest {
            val result = MangaPagingSource { _, _ -> throw IOException("offline") }.load(refresh(30))
            assertTrue(result is PagingSource.LoadResult.Error)
        }

    private fun manga(id: String) =
        Manga(
            id = id,
            title = mapOf("en" to id),
            altTitles = emptyList(),
            description = emptyMap(),
            originalLanguage = "ja",
            status = null,
            demographic = null,
            contentRating = pt.aguiarvieira.m3mangadex.core.model.ContentRating.Safe,
            year = null,
            tags = emptyList(),
            authors = emptyList(),
            artists = emptyList(),
            coverFileName = null,
            availableLanguages = emptyList(),
            lastVolume = null,
            lastChapter = null,
            updatedAt = null,
        )
}
