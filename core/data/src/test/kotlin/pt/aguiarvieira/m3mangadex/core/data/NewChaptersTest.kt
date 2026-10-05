package pt.aguiarvieira.m3mangadex.core.data

import org.junit.Assert.assertEquals
import org.junit.Test
import pt.aguiarvieira.m3mangadex.core.data.notify.newChapters
import pt.aguiarvieira.m3mangadex.core.model.Chapter
import pt.aguiarvieira.m3mangadex.core.model.FeedEntry
import java.time.Instant

class NewChaptersTest {
    private fun entry(
        manga: String,
        number: String,
        at: Long,
        id: String = "$manga-$number-$at",
    ) = FeedEntry(Chapter(id, manga, null, number, null, "en", 10, null, Instant.ofEpochMilli(at), emptyList()), null)

    @Test
    fun `only chapters after the last check, grouped by manga, newest manga first`() {
        val feed =
            listOf(
                entry("a", "10", at = 500),
                entry("b", "3", at = 900),
                entry("a", "11", at = 1_000),
                entry("c", "1", at = 50),
            )
        val groups = newChapters(feed, since = 100)
        assertEquals(listOf("a", "b"), groups.map { it.mangaId })
        assertEquals(listOf("11", "10"), groups.first().entries.map { it.chapter.number })
    }

    @Test
    fun `several uploads of one chapter count once`() {
        val feed = listOf(entry("a", "5", at = 200, id = "en"), entry("a", "5", at = 300, id = "pt-br"))
        assertEquals(1, newChapters(feed, since = 100).single().entries.size)
    }
}
