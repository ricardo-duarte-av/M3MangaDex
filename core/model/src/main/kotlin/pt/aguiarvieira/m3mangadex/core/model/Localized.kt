package pt.aguiarvieira.m3mangadex.core.model

/** Picking one string out of MangaDex's language-keyed maps. */
object Localized {
    /** The first of [languages] present, then English, then whatever there is. */
    fun pick(
        text: LocalizedText,
        languages: List<String>,
    ): String? =
        (languages + "en").firstNotNullOfOrNull { text[it]?.takeIf(String::isNotBlank) }
            ?: text.values.firstOrNull(String::isNotBlank)

    /**
     * A title for [languages]: the main title if it is in one of them, else a matching alternative
     * title (so "Solo Leveling" rather than "Na Honjaman Level-Up" for English readers), else the
     * main title in whatever language it has.
     */
    fun title(
        title: LocalizedText,
        altTitles: List<LocalizedText>,
        languages: List<String>,
    ): String {
        for (language in languages + "en") {
            title[language]?.takeIf(String::isNotBlank)?.let { return it }
            altTitles.firstNotNullOfOrNull { it[language]?.takeIf(String::isNotBlank) }?.let { return it }
        }
        return title.values.firstOrNull(String::isNotBlank)
            ?: altTitles.firstNotNullOfOrNull { it.values.firstOrNull() }
            ?: ""
    }
}
