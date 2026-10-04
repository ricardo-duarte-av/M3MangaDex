package pt.aguiarvieira.m3mangadex.core.network

import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import pt.aguiarvieira.m3mangadex.core.model.ContentRating
import pt.aguiarvieira.m3mangadex.core.model.Covers
import pt.aguiarvieira.m3mangadex.core.model.MangaFilter
import pt.aguiarvieira.m3mangadex.core.model.MangaOrder
import pt.aguiarvieira.m3mangadex.core.model.PublicationStatus
import pt.aguiarvieira.m3mangadex.core.model.coverUrl
import pt.aguiarvieira.m3mangadex.core.network.di.NetworkModule

class MangaDexApiTest {
    private val server = MockWebServer()
    private lateinit var api: MangaDexApi

    @Before
    fun setUp() {
        server.start()
        val client = OkHttpClient.Builder().addInterceptor(UserAgentInterceptor("M3MangaDex/test")).build()
        api = MangaDexApi(client, NetworkModule.json(), server.url("/"))
    }

    @After
    fun tearDown() = server.close()

    private fun fixture(name: String) = javaClass.getResource("/fixtures/$name")!!.readText()

    private fun enqueue(
        body: String,
        code: Int = 200,
    ) = server.enqueue(
        MockResponse
            .Builder()
            .code(code)
            .body(body)
            .build()
    )

    @Test
    fun `search builds MangaDex array parameters and parses the list`() =
        runTest {
            enqueue(fixture("manga_list.json"))
            val filter =
                MangaFilter(
                    title = " solo ",
                    status = setOf(PublicationStatus.Completed),
                    contentRating = setOf(ContentRating.Safe, ContentRating.Suggestive),
                    order = MangaOrder.Rating,
                )

            val page = api.searchManga(filter, offset = 20, limit = 2)

            val url = server.takeRequest().url
            assertEquals("/manga", url.encodedPath)
            assertEquals("solo", url.queryParameter("title"))
            assertEquals("20", url.queryParameter("offset"))
            assertEquals(listOf("cover_art"), url.queryParameterValues("includes[]"))
            assertEquals(listOf("completed"), url.queryParameterValues("status[]"))
            assertEquals(listOf("safe", "suggestive"), url.queryParameterValues("contentRating[]"))
            assertEquals("desc", url.queryParameter("order[rating]"))

            assertEquals(85826, page.total)
            assertEquals(2, page.items.size)
            val solo = page.items[0]
            assertEquals("Solo Leveling", solo.displayTitle(listOf("en")))
            assertEquals(PublicationStatus.Completed, solo.status)
            assertEquals(3, solo.tags.size)
            assertTrue(solo.authors.isNotEmpty())
            assertTrue(solo.coverUrl(Covers.Size.Medium)!!.endsWith(".jpg.512.jpg"))
        }

    @Test
    fun `tolerates an empty description sent as an array and null languages`() =
        runTest {
            enqueue(fixture("manga_list.json"))
            val second = api.searchManga(MangaFilter(), 0, 2).items[1]
            assertTrue(second.description.isEmpty())
            assertFalse(second.availableLanguages.contains("null"))
        }

    @Test
    fun `without a title, relevance falls back to follows`() =
        runTest {
            enqueue(fixture("manga_list.json"))
            api.searchManga(MangaFilter(order = MangaOrder.Relevance), 0, 2)
            val url = server.takeRequest().url
            assertEquals("desc", url.queryParameter("order[followedCount]"))
            assertNull(url.queryParameter("order[relevance]"))
        }

    @Test
    fun `chapter feed maps groups and external chapters`() =
        runTest {
            enqueue(fixture("chapter_feed.json"))
            val page = api.chapters("m1", listOf("en"), ContentRating.entries, offset = 0, limit = 3)
            val request = server.takeRequest()
            assertEquals("/manga/m1/feed", request.url.encodedPath)
            assertEquals(listOf("en"), request.url.queryParameterValues("translatedLanguage[]"))
            assertEquals("M3MangaDex/test", request.headers["User-Agent"])

            val first = page.items.first()
            assertEquals("15", first.number)
            assertEquals("Dungeon Boss", first.title)
            assertTrue(first.isExternal)
            assertEquals("Webnovel", first.groups.single().name)
            assertTrue(first.groups.single().official)
            assertEquals(24, page.total)
        }

    @Test
    fun `errors carry MangaDex's detail`() =
        runTest {
            enqueue("""{"result":"error","errors":[{"status":404,"title":"not_found","detail":"Manga could not be found"}]}""", code = 404)
            val error = runCatching { api.manga("nope") }.exceptionOrNull() as MangaDexException
            assertEquals(404, error.status)
            assertEquals("Manga could not be found", error.message)
        }
}
