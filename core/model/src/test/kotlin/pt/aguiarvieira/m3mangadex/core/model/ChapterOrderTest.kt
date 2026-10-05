package pt.aguiarvieira.m3mangadex.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChapterOrderTest {
    private fun chapter(
        id: String,
        number: String?,
        group: String = "a",
    ) = Chapter(id, "m", null, number, null, "en", 10, null, null, listOf(ScanlationGroup(group, group, false)))

    private val chapters =
        listOf(
            chapter("1", "1"),
            chapter("2a", "2", group = "a"),
            chapter("2b", "2", group = "b"),
            chapter("2.5", "2.5", group = "b"),
            chapter("10", "10"),
            chapter("x", null),
        )

    @Test
    fun `orders numerically, not textually`() {
        assertEquals(listOf("x", "1", "2a", "2b", "2.5", "10"), chapters.sortedWith(ChapterOrder).map { it.id })
    }

    @Test
    fun `next skips same-number uploads and prefers the reader's group`() {
        val fromOne = neighbors(chapters, chapters[0])
        assertNull(fromOne.previous)
        assertEquals("2a", fromOne.next?.id)

        val fromB = neighbors(chapters, chapters[2])
        assertEquals("2.5", fromB.next?.id)
        assertEquals("1", fromB.previous?.id)

        val fromHalf = neighbors(chapters, chapters[3])
        assertEquals("2b", fromHalf.previous?.id)
        assertEquals("10", fromHalf.next?.id)
    }

    @Test
    fun `last chapter has no next, oneshots have no neighbours`() {
        assertNull(neighbors(chapters, chapters[4]).next)
        assertEquals(ChapterNeighbors(null, null), neighbors(chapters, chapters[5]))
    }

    @Test
    fun `page urls use data saver only when every page has one`() {
        val server = AtHomeServer("https://node.example/", "h", listOf("a.png", "b.png"), listOf("a.jpg", "b.jpg"))
        assertEquals("https://node.example/data/h/a.png", server.pageUrl(0, dataSaver = false))
        assertEquals("https://node.example/data-saver/h/b.jpg", server.pageUrl(1, dataSaver = true))
        val noSaver = server.copy(dataSaver = emptyList())
        assertEquals("https://node.example/data/h/b.png", noSaver.pageUrl(1, dataSaver = true))
    }
}
