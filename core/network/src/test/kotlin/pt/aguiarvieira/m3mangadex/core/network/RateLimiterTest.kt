package pt.aguiarvieira.m3mangadex.core.network

import org.junit.Assert.assertEquals
import org.junit.Test

class RateLimiterTest {
    private var now = 0L
    private val sleeps = mutableListOf<Long>()
    private val limiter =
        RateLimiter(permits = 2, periodNanos = 1_000, clock = { now }) {
            sleeps += it
            now += it
        }

    @Test
    fun `permits pass until the window is full, then wait for the oldest to expire`() {
        limiter.acquire()
        now = 100
        limiter.acquire()
        now = 200
        limiter.acquire()
        assertEquals(listOf(800L), sleeps)
        assertEquals(1_000L, now)
    }

    @Test
    fun `a quiet period frees the window`() {
        limiter.acquire()
        limiter.acquire()
        now = 5_000
        limiter.acquire()
        limiter.acquire()
        assertEquals(emptyList<Long>(), sleeps)
    }
}
