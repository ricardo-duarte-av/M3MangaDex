package pt.aguiarvieira.m3mangadex.core.network

import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Response

/** MangaDex requires an honest User-Agent on every request, and rejects proxied (`Via`) ones. */
class UserAgentInterceptor(
    private val userAgent: String,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response =
        chain.proceed(
            chain
                .request()
                .newBuilder()
                .header("User-Agent", userAgent)
                .removeHeader("Via")
                .build(),
        )
}

/**
 * Paces requests to the hosts [limits] matches through [limiter], and when MangaDex still answers
 * 429, waits until its `X-RateLimit-Retry-After` (epoch seconds) and tries again, a few times.
 */
class RateLimitInterceptor(
    private val limiter: RateLimiter,
    private val limits: (HttpUrl) -> Boolean,
    private val nowMillis: () -> Long = System::currentTimeMillis,
    private val sleepMillis: (Long) -> Unit = Thread::sleep,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (!limits(request.url)) return chain.proceed(request)
        var attempt = 0
        while (true) {
            limiter.acquire()
            val response = chain.proceed(request)
            if (response.code != TOO_MANY_REQUESTS || attempt >= MAX_RETRIES) return response
            val retryAt = response.header("X-RateLimit-Retry-After")?.toLongOrNull()?.times(MILLIS_PER_SECOND)
            val wait = ((retryAt ?: 0L) - nowMillis()).coerceIn(MIN_WAIT_MILLIS, MAX_WAIT_MILLIS)
            response.close()
            sleepMillis(wait)
            attempt++
        }
    }

    private companion object {
        const val TOO_MANY_REQUESTS = 429
        const val MAX_RETRIES = 2
        const val MILLIS_PER_SECOND = 1_000L
        const val MIN_WAIT_MILLIS = 1_000L
        const val MAX_WAIT_MILLIS = 10_000L
    }
}
