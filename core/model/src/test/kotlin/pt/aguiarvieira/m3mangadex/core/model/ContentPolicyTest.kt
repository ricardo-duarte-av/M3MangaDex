package pt.aguiarvieira.m3mangadex.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ContentPolicyTest {
    @Test
    fun `the play policy drops erotica, and never leaves nothing`() {
        assertEquals(setOf(ContentRating.Safe), ContentPolicy.Play.allowed(setOf(ContentRating.Safe, ContentRating.Erotica)))
        assertEquals(ContentRating.Default, ContentPolicy.Play.allowed(setOf(ContentRating.Erotica)))
    }

    @Test
    fun `nobody gets pornographic`() {
        assertEquals(ContentRating.Default, ContentPolicy.Open.allowed(setOf(ContentRating.Pornographic)))
    }
}
