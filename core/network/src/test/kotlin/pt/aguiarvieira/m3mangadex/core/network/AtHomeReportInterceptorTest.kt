package pt.aguiarvieira.m3mangadex.core.network

import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pt.aguiarvieira.m3mangadex.core.network.di.NetworkModule
import java.util.concurrent.TimeUnit

class AtHomeReportInterceptorTest {
    private val node = MockWebServer().apply { start() }
    private val reports = MockWebServer().apply { start() }

    @After
    fun tearDown() {
        node.close()
        reports.close()
    }

    @Test
    fun `recognises at-home page urls but not MangaDex's own hosts or covers`() {
        assertTrue(AtHomeReportInterceptor.isAtHomePage("https://abc.xyz.mangadex.network:443/token/data/hash/1.png".toHttpUrl()))
        assertTrue(AtHomeReportInterceptor.isAtHomePage("https://node.example/data-saver/hash/1.jpg".toHttpUrl()))
        assertFalse(AtHomeReportInterceptor.isAtHomePage("https://uploads.mangadex.org/data/hash/1.png".toHttpUrl()))
        assertFalse(AtHomeReportInterceptor.isAtHomePage("https://uploads.mangadex.org/covers/m/f.jpg.256.jpg".toHttpUrl()))
    }

    @Test
    fun `reports each page fetched from a node`() {
        node.enqueue(
            MockResponse
                .Builder()
                .code(200)
                .addHeader("X-Cache", "HIT")
                .body("12345")
                .build()
        )
        reports.enqueue(MockResponse.Builder().code(200).build())
        val json = NetworkModule.json()
        val client =
            OkHttpClient
                .Builder()
                .addInterceptor(AtHomeReportInterceptor(OkHttpClient(), json, reports.url("/report"), nowMillis = { 1_000L }))
                .build()

        client.newCall(Request.Builder().url(node.url("/data/hash/1.png")).build()).execute().use { it.body.string() }

        val report = reports.takeRequest(5, TimeUnit.SECONDS)!!
        val body = report.body!!.utf8()
        assertEquals("POST", report.method)
        assertTrue(body, body.contains("\"success\":true"))
        assertTrue(body, body.contains("\"bytes\":5"))
        assertTrue(body, body.contains("\"cached\":true"))
        assertTrue(body, body.contains("/data/hash/1.png"))
    }
}
