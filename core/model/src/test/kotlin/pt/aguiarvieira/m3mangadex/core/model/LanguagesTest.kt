package pt.aguiarvieira.m3mangadex.core.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class LanguagesTest {
    @Test
    fun `maps locales to MangaDex codes`() {
        assertEquals("pt", Languages.fromLocale(Locale.forLanguageTag("pt-PT")))
        assertEquals("pt-br", Languages.fromLocale(Locale.forLanguageTag("pt-BR")))
        assertEquals("es-la", Languages.fromLocale(Locale.forLanguageTag("es-MX")))
        assertEquals("es", Languages.fromLocale(Locale.forLanguageTag("es-ES")))
        assertEquals("zh-hk", Languages.fromLocale(Locale.forLanguageTag("zh-TW")))
        assertEquals("zh", Languages.fromLocale(Locale.forLanguageTag("zh-CN")))
        assertEquals("en", Languages.fromLocale(Locale.forLanguageTag("en-GB")))
    }

    @Test
    fun `defaults add English once`() {
        assertEquals(listOf("pt", "en"), Languages.defaults(Locale.forLanguageTag("pt-PT")))
        assertEquals(listOf("en"), Languages.defaults(Locale.US))
    }
}
