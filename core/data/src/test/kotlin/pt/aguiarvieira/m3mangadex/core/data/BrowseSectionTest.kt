package pt.aguiarvieira.m3mangadex.core.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import pt.aguiarvieira.m3mangadex.core.model.MangaOrder
import java.time.Instant

class BrowseSectionTest {
    private val now = Instant.parse("2026-10-05T12:34:56Z")

    @Test
    fun `popular new looks back thirty days in MangaDex's date format`() {
        val filter = BrowseSection.PopularNew.filter(listOf("en"), now)
        assertEquals("2026-09-05T12:00:00", filter.createdAtSince)
        assertEquals(MangaOrder.Follows, filter.order)
        assertEquals(setOf("en"), filter.availableLanguages)
    }

    @Test
    fun `other sections are plain orders`() {
        assertEquals(MangaOrder.LatestUpload, BrowseSection.LatestUpdates.filter(emptyList(), now).order)
        assertEquals(MangaOrder.Rating, BrowseSection.TopRated.filter(emptyList(), now).order)
        assertNull(BrowseSection.TopRated.filter(emptyList(), now).createdAtSince)
    }
}
