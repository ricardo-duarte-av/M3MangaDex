package pt.aguiarvieira.m3mangadex.core.network

import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

class RateLimitInterceptorTest {
    private val server = MockWebServer().apply { start() }
    private val sleeps = mutableListOf<Long>()

    @After
    fun tearDown() = server.close()

    private fun client(limitsAll: Boolean) =
        OkHttpClient
            .Builder()
            .addInterceptor(
                RateLimitInterceptor(
                    limiter = RateLimiter(permits = 100, periodNanos = 1),
                    limits = { limitsAll },
                    nowMillis = { 1_000_000L },
                    sleepMillis = { sleeps += it },
                ),
            ).build()

    @Test
    fun `waits until Retry-After on 429, then retries`() {
        server.enqueue(
            MockResponse
                .Builder()
                .code(429)
                .addHeader("X-RateLimit-Retry-After", "1003")
                .build()
        )
        server.enqueue(
            MockResponse
                .Builder()
                .code(200)
                .body("ok")
                .build()
        )

        val response = client(limitsAll = true).newCall(Request.Builder().url(server.url("/")).build()).execute()

        assertEquals(200, response.code)
        assertEquals(listOf(3_000L), sleeps)
        assertEquals(2, server.requestCount)
    }

    @Test
    fun `gives up after two retries`() {
        repeat(3) { server.enqueue(MockResponse.Builder().code(429).build()) }
        val response = client(limitsAll = true).newCall(Request.Builder().url(server.url("/")).build()).execute()
        assertEquals(429, response.code)
        assertEquals(listOf(1_000L, 1_000L), sleeps)
    }

    @Test
    fun `other hosts pass straight through`() {
        server.enqueue(MockResponse.Builder().code(429).build())
        val response = client(limitsAll = false).newCall(Request.Builder().url(server.url("/")).build()).execute()
        assertEquals(429, response.code)
        assertEquals(emptyList<Long>(), sleeps)
    }
}
