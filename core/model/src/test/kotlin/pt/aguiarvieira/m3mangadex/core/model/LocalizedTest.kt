package pt.aguiarvieira.m3mangadex.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class LocalizedTest {
    private val title = mapOf("ko-ro" to "Na Honjaman Level-Up")
    private val alt = listOf(mapOf("ko" to "나 혼자만 레벨업"), mapOf("en" to "Solo Leveling"), mapOf("pt-br" to "Nível Solo"))

    @Test
    fun `alternative title in the preferred language wins over a romanized main title`() {
        assertEquals("Nível Solo", Localized.title(title, alt, listOf("pt-br")))
    }

    @Test
    fun `falls back to English`() {
        assertEquals("Solo Leveling", Localized.title(title, alt, listOf("fr")))
    }

    @Test
    fun `main title when nothing matches`() {
        assertEquals("Na Honjaman Level-Up", Localized.title(title, emptyList(), listOf("fr")))
    }

    @Test
    fun `main title in the preferred language beats alternatives`() {
        assertEquals("Main", Localized.title(mapOf("en" to "Main"), listOf(mapOf("en" to "Alt")), listOf("en")))
    }

    @Test
    fun `pick prefers languages in order and skips blanks`() {
        val text = mapOf("en" to "English", "pt-br" to " ", "de" to "Deutsch")
        assertEquals("Deutsch", Localized.pick(text, listOf("pt-br", "de")))
        assertEquals("English", Localized.pick(text, listOf("fr")))
    }
}
