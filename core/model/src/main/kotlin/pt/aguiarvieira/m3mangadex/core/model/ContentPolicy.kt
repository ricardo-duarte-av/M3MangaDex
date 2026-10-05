package pt.aguiarvieira.m3mangadex.core.model

/**
 * Which content ratings this build may ever show. The Play build stops at suggestive (Google Play
 * bans sexually explicit content); the sideloaded GitHub build lets users opt into erotica.
 * Pornographic is never offered.
 */
data class ContentPolicy(
    val selectable: List<ContentRating>,
) {
    /** [ratings] limited to what this build allows; the defaults when nothing is left. */
    fun allowed(ratings: Set<ContentRating>): Set<ContentRating> =
        ratings.filter { it in selectable }.toSet().ifEmpty { ContentRating.Default }

    companion object {
        val Play = ContentPolicy(listOf(ContentRating.Safe, ContentRating.Suggestive))
        val Open = ContentPolicy(listOf(ContentRating.Safe, ContentRating.Suggestive, ContentRating.Erotica))
    }
}
