package pt.aguiarvieira.m3mangadex.core.model

import java.time.Instant

/** A language-keyed text, as MangaDex returns titles and descriptions (`"en"`, `"ja-ro"`, …). */
typealias LocalizedText = Map<String, String>

data class Manga(
    val id: String,
    val title: LocalizedText,
    val altTitles: List<LocalizedText>,
    val description: LocalizedText,
    val originalLanguage: String,
    val status: PublicationStatus?,
    val demographic: Demographic?,
    val contentRating: ContentRating,
    val year: Int?,
    val tags: List<Tag>,
    val authors: List<String>,
    val artists: List<String>,
    /** File name of the main cover, resolved through [Covers]; null when the cover wasn't included. */
    val coverFileName: String?,
    val availableLanguages: List<String>,
    val lastVolume: String?,
    val lastChapter: String?,
    val updatedAt: Instant?,
) {
    /** Title in the first of [languages] that has one (main title, then alternatives), else the main title. */
    fun displayTitle(languages: List<String>): String = Localized.title(title, altTitles, languages)

    fun displayDescription(languages: List<String>): String = Localized.pick(description, languages).orEmpty()
}

data class MangaStats(
    val follows: Int?,
    /** Bayesian average, 0–10. */
    val rating: Double?,
)

enum class PublicationStatus(
    val apiValue: String,
) {
    Ongoing("ongoing"),
    Completed("completed"),
    Hiatus("hiatus"),
    Cancelled("cancelled"),
    ;

    companion object {
        fun of(value: String?) = entries.firstOrNull { it.apiValue == value }
    }
}

enum class Demographic(
    val apiValue: String,
) {
    Shounen("shounen"),
    Shoujo("shoujo"),
    Seinen("seinen"),
    Josei("josei"),
    ;

    companion object {
        fun of(value: String?) = entries.firstOrNull { it.apiValue == value }
    }
}

enum class ContentRating(
    val apiValue: String,
) {
    Safe("safe"),
    Suggestive("suggestive"),
    Erotica("erotica"),
    Pornographic("pornographic"),
    ;

    companion object {
        fun of(value: String?) = entries.firstOrNull { it.apiValue == value } ?: Safe

        /** What the app shows unless the user opts into more. */
        val Default = setOf(Safe, Suggestive)

        /** What the user may opt into. Pornographic stays out of the Play build (policy). */
        val Selectable = listOf(Safe, Suggestive, Erotica)
    }
}

data class Tag(
    val id: String,
    val name: String,
    val group: TagGroup,
)

enum class TagGroup(
    val apiValue: String,
) {
    Genre("genre"),
    Theme("theme"),
    Format("format"),
    Content("content"),
    ;

    companion object {
        fun of(value: String?) = entries.firstOrNull { it.apiValue == value } ?: Genre
    }
}
