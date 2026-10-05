package pt.aguiarvieira.m3mangadex.core.network

import kotlinx.serialization.json.Json
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import pt.aguiarvieira.m3mangadex.core.network.dto.ImageReportDto
import java.io.IOException

/**
 * MangaDex@Home nodes are volunteer servers; MangaDex asks clients to report every page fetched
 * from one (success, size, time, cache hit) so it can route around slow or broken nodes. Pages
 * served by MangaDex itself (`*.mangadex.org`) are not reported. Reports are fire-and-forget on
 * [reporter], which must not carry this interceptor.
 */
class AtHomeReportInterceptor(
    private val reporter: OkHttpClient,
    private val json: Json,
    private val reportUrl: HttpUrl = REPORT_URL,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val url = chain.request().url
        if (!isAtHomePage(url)) return chain.proceed(chain.request())
        val start = nowMillis()
        val response =
            try {
                chain.proceed(chain.request())
            } catch (e: IOException) {
                report(
                    ImageReportDto(
                        url.toString(),
                        success = false,
                        bytes = 0,
                        duration = nowMillis() - start,
                        cached = false
                    )
                )
                throw e
            }
        report(
            ImageReportDto(
                url = url.toString(),
                success = response.isSuccessful,
                bytes = response.body.contentLength().coerceAtLeast(0),
                duration = nowMillis() - start,
                cached = response.header("X-Cache")?.startsWith("HIT", ignoreCase = true) == true,
            ),
        )
        return response
    }

    private fun report(report: ImageReportDto) {
        val body = json.encodeToString(ImageReportDto.serializer(), report).toRequestBody(JSON)
        reporter
            .newCall(
                Request
                    .Builder()
                    .url(reportUrl)
                    .post(body)
                    .build()
            ).enqueue(
                object : Callback {
                    override fun onFailure(
                        call: Call,
                        e: IOException,
                    ) = Unit // Best effort: a lost report costs MangaDex one data point.

                    override fun onResponse(
                        call: Call,
                        response: Response,
                    ) = response.close()
                },
            )
    }

    companion object {
        val REPORT_URL = "https://api.mangadex.network/report".toHttpUrl()
        private val JSON = "application/json".toMediaType()
        private val QUALITIES = setOf("data", "data-saver")

        /** `{node}/{data|data-saver}/{hash}/{file}` on a host that isn't MangaDex's own. */
        fun isAtHomePage(url: HttpUrl): Boolean {
            val segments = url.pathSegments
            return segments.size >= PAGE_PATH_SEGMENTS &&
                segments[segments.size - PAGE_PATH_SEGMENTS] in QUALITIES &&
                !(url.host == "mangadex.org" || url.host.endsWith(".mangadex.org"))
        }

        private const val PAGE_PATH_SEGMENTS = 3
    }
}
