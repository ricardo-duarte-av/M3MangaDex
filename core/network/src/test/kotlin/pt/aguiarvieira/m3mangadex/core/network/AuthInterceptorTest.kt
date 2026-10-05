package pt.aguiarvieira.m3mangadex.core.network

import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import pt.aguiarvieira.m3mangadex.core.model.MangaFilter
import pt.aguiarvieira.m3mangadex.core.network.di.NetworkModule

class AuthInterceptorTest {
    private val server = MockWebServer().apply { start() }

    /** Hands out token-N; a rejection of the current one moves to the next. */
    private val tokens =
        object : AccessTokenProvider {
            var current = 1
            var refreshes = 0

            override fun accessToken() = "token-$current"

            override fun refreshAfterRejection(rejected: String): String {
                if (rejected == "token-$current") {
                    current++
                    refreshes++
                }
                return "token-$current"
            }
        }

    private val client =
        OkHttpClient
            .Builder()
            .addInterceptor(AuthInterceptor(tokens))
            .authenticator(TokenAuthenticator(tokens))
            .build()
    private val json = NetworkModule.json()

    @After
    fun tearDown() = server.close()

    @Test
    fun `public requests never carry the token`() =
        runTest {
            server.enqueue(
                MockResponse
                    .Builder()
                    .code(200)
                    .body("""{"data":[],"total":0}""")
                    .build()
            )
            MangaDexApi(client, json, server.url("/")).searchManga(MangaFilter(), 0, 10)
            assertNull(server.takeRequest().headers["Authorization"])
        }

    @Test
    fun `user requests carry it, and a 401 refreshes once and retries`() =
        runTest {
            server.enqueue(MockResponse.Builder().code(401).build())
            server.enqueue(
                MockResponse
                    .Builder()
                    .code(200)
                    .body("""{"result":"ok","statuses":{"m1":"reading"}}""")
                    .build()
            )
            val statuses = MangaDexUserApi(client, json, server.url("/")).statuses()

            assertEquals("Bearer token-1", server.takeRequest().headers["Authorization"])
            assertEquals("Bearer token-2", server.takeRequest().headers["Authorization"])
            assertEquals(1, tokens.refreshes)
            assertEquals(mapOf("m1" to pt.aguiarvieira.m3mangadex.core.model.ReadingStatus.Reading), statuses)
        }

    @Test
    fun `a second 401 gives up instead of looping`() =
        runTest {
            repeat(3) { server.enqueue(MockResponse.Builder().code(401).build()) }
            val result = runCatching { MangaDexUserApi(client, json, server.url("/")).statuses() }
            assertEquals(401, (result.exceptionOrNull() as MangaDexException).status)
            assertEquals(2, server.requestCount)
        }

    @Test
    fun `marking chapters read posts both lists`() =
        runTest {
            server.enqueue(
                MockResponse
                    .Builder()
                    .code(200)
                    .body("""{"result":"ok"}""")
                    .build()
            )
            MangaDexUserApi(client, json, server.url("/")).markChapters("m1", read = listOf("c1"), unread = listOf("c2"))
            val request = server.takeRequest()
            assertEquals("/manga/m1/read", request.url.encodedPath)
            assertNull(request.url.queryParameter("updateHistory"))
            assertEquals("""{"chapterIdsRead":["c1"],"chapterIdsUnread":["c2"]}""", request.body!!.utf8())
        }
}
