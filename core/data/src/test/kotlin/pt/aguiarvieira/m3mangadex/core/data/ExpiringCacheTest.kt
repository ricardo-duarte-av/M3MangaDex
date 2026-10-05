package pt.aguiarvieira.m3mangadex.core.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExpiringCacheTest {
    private var now = 0L
    private val cache = ExpiringCache<String, Int>(maxSize = 2, ttlMillis = 100) { now }

    @Test
    fun `values expire`() {
        cache["a"] = 1
        now = 100
        assertEquals(1, cache["a"])
        now = 101
        assertNull(cache["a"])
    }

    @Test
    fun `least recently used goes first`() {
        cache["a"] = 1
        cache["b"] = 2
        cache["a"]
        cache["c"] = 3
        assertNull(cache["b"])
        assertEquals(1, cache["a"])
        assertEquals(3, cache["c"])
    }
}
