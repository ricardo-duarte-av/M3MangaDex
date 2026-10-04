package pt.aguiarvieira.m3mangadex.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PageTest {
    @Test
    fun `next offset until the total is reached`() {
        assertEquals(30, Page(List(30) { it }, offset = 0, total = 100).nextOffset)
        assertNull(Page(List(10) { it }, offset = 90, total = 100).nextOffset)
        assertNull(Page(emptyList<Int>(), offset = 0, total = 100).nextOffset)
    }
}
