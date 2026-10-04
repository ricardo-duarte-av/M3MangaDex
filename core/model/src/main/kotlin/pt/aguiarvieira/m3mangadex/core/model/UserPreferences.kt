package pt.aguiarvieira.m3mangadex.core.model

data class UserPreferences(
    /** MangaDex language codes chapters are shown in, in order of preference. */
    val chapterLanguages: List<String>,
    val contentRatings: Set<ContentRating>,
    /** Load compressed page images (M2). */
    val dataSaver: Boolean,
)
