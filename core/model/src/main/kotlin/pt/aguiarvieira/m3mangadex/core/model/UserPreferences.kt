package pt.aguiarvieira.m3mangadex.core.model

data class UserPreferences(
    /** MangaDex language codes chapters are shown in, in order of preference. */
    val chapterLanguages: List<String>,
    val contentRatings: Set<ContentRating>,
    /** Load compressed page images. */
    val dataSaver: Boolean,
    /** Volume up/down turn pages in the reader. */
    val volumeKeyPaging: Boolean = false,
    /** Show two pages side by side on wide screens (paged modes). */
    val doublePageSpreads: Boolean = true,
)
