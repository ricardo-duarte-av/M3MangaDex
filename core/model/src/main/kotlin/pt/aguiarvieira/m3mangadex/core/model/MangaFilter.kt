package pt.aguiarvieira.m3mangadex.core.model

/** A `/manga` query: everything the search screen and the browse sections can ask for. */
data class MangaFilter(
    val title: String = "",
    val includedTags: Set<String> = emptySet(),
    val excludedTags: Set<String> = emptySet(),
    val status: Set<PublicationStatus> = emptySet(),
    val demographic: Set<Demographic> = emptySet(),
    /** Empty means "the user's preference"; the repository fills it in. */
    val contentRating: Set<ContentRating> = emptySet(),
    val originalLanguage: Set<String> = emptySet(),
    /** Only titles with chapters in one of these languages; empty means any. */
    val availableLanguages: Set<String> = emptySet(),
    val order: MangaOrder = MangaOrder.Relevance,
    /** ISO-8601 without offset (MangaDex's format), e.g. `2026-09-04T00:00:00`. */
    val createdAtSince: String? = null,
) {
    /** Number of filters set beyond the title and sort order, for a badge. */
    val activeCount: Int
        get() =
            listOf(includedTags, excludedTags, status, demographic, contentRating, originalLanguage)
                .count { it.isNotEmpty() }
}

/** Sort orders MangaDex supports; [field] is the `order[…]` key, always descending except title. */
enum class MangaOrder(
    val field: String,
    val ascending: Boolean = false,
) {
    Relevance("relevance"),
    Follows("followedCount"),
    Rating("rating"),
    LatestUpload("latestUploadedChapter"),
    Created("createdAt"),
    Year("year"),
    Title("title", ascending = true),
}
