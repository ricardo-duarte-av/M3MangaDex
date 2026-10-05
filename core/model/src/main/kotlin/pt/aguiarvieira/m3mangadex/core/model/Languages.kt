package pt.aguiarvieira.m3mangadex.core.model

import java.util.Locale

/** MangaDex's language codes: mostly ISO 639-1, with a few regional variants of its own. */
object Languages {
    /** The languages most chapters are translated into, offered first in settings. */
    val common =
        listOf(
            "en",
            "pt-br",
            "pt",
            "es",
            "es-la",
            "fr",
            "de",
            "it",
            "ru",
            "pl",
            "tr",
            "id",
            "vi",
            "th",
            "ar",
            "uk",
            "ja",
            "ko",
            "zh",
            "zh-hk",
        )

    /**
     * MangaDex splits a few languages by region. Readers of one variant can read the other, and
     * scanlation tends to cluster in one of them (almost all Portuguese is `pt-br`), so they go
     * together.
     */
    private val regionalSiblings =
        mapOf(
            "pt" to "pt-br",
            "pt-br" to "pt",
            "es" to "es-la",
            "es-la" to "es",
            "zh" to "zh-hk",
            "zh-hk" to "zh",
        )

    private val latinAmerica =
        setOf(
            "MX",
            "AR",
            "CO",
            "CL",
            "PE",
            "VE",
            "EC",
            "GT",
            "CU",
            "BO",
            "DO",
            "HN",
            "PY",
            "SV",
            "NI",
            "CR",
            "PA",
            "UY",
            "PR",
            "US",
            "419",
        )

    /** The MangaDex code for [locale]: `pt-BR` → `pt-br`, `es-MX` → `es-la`, `zh-TW` → `zh-hk`. */
    fun fromLocale(locale: Locale): String {
        val language = locale.language.lowercase()
        val region = locale.country.uppercase()
        return when (language) {
            "pt" -> if (region == "BR") "pt-br" else "pt"
            "es" -> if (region in latinAmerica) "es-la" else "es"
            "zh" -> if (region == "TW" || region == "HK" || locale.script == "Hant") "zh-hk" else "zh"
            "in" -> "id"
            "iw" -> "he"
            else -> language
        }
    }

    /** First run: the device's language, its regional sibling, then English. */
    fun defaults(locale: Locale): List<String> {
        val own = fromLocale(locale)
        return listOfNotNull(own, regionalSiblings[own], "en").distinct()
    }

    /** A human name for [code], in [display]'s language. */
    fun displayName(
        code: String,
        display: Locale,
    ): String =
        when (code) {
            "es-la" -> Locale.forLanguageTag("es-419").getDisplayName(display)
            "zh-hk" -> Locale.forLanguageTag("zh-Hant").getDisplayName(display)
            else -> Locale.forLanguageTag(code).getDisplayName(display)
        }.replaceFirstChar { it.titlecase(display) }
}
