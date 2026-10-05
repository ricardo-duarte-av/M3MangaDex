package pt.aguiarvieira.m3mangadex.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ReaderLayoutTest {
    @Test
    fun `cover alone, then pairs`() {
        assertEquals(listOf(listOf(0), listOf(1, 2), listOf(3, 4), listOf(5)), spreads(6, emptySet()))
    }

    @Test
    fun `wide pages stand alone and pairing restarts after them`() {
        assertEquals(
            listOf(listOf(0), listOf(1), listOf(2), listOf(3, 4), listOf(5)),
            spreads(6, wide = setOf(2)),
        )
    }

    @Test
    fun `spreadOf finds the spread holding a page`() {
        val layout = spreads(6, emptySet())
        assertEquals(0, layout.spreadOf(0))
        assertEquals(2, layout.spreadOf(4))
    }

    @Test
    fun `default modes follow the original language and long-strip tag`() {
        fun manga(
            language: String,
            tags: List<Tag> = emptyList(),
        ) = Manga(
            "m",
            emptyMap(),
            emptyList(),
            emptyMap(),
            language,
            null,
            null,
            ContentRating.Safe,
            null,
            tags,
            emptyList(),
            emptyList(),
            null,
            emptyList(),
            null,
            null,
            null,
        )
        assertEquals(ReaderMode.RightToLeft, ReaderMode.defaultFor(manga("ja")))
        assertEquals(ReaderMode.Webtoon, ReaderMode.defaultFor(manga("ko")))
        assertEquals(ReaderMode.LeftToRight, ReaderMode.defaultFor(manga("en")))
        val longStrip = Tag(ReaderMode.LONG_STRIP_TAG, "Long Strip", TagGroup.Format)
        assertEquals(ReaderMode.Webtoon, ReaderMode.defaultFor(manga("ja", listOf(longStrip))))
    }
}
