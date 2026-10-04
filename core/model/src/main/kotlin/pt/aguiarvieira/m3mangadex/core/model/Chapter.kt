package pt.aguiarvieira.m3mangadex.core.model

import java.time.Instant

data class Chapter(
    val id: String,
    val mangaId: String?,
    /** As MangaDex stores them: free-form strings ("1", "10.5", "Extra"), null when unnumbered. */
    val volume: String?,
    val number: String?,
    val title: String?,
    val language: String,
    val pages: Int,
    /** Set for chapters hosted elsewhere (official publishers); those have no pages on MangaDex. */
    val externalUrl: String?,
    val readableAt: Instant?,
    val groups: List<ScanlationGroup>,
) {
    val isExternal: Boolean get() = externalUrl != null && pages == 0
}

data class ScanlationGroup(
    val id: String,
    val name: String,
    val official: Boolean,
)

/** One page of an offset-paginated MangaDex collection. */
data class Page<T>(
    val items: List<T>,
    val offset: Int,
    val total: Int,
) {
    val nextOffset: Int? get() = (offset + items.size).takeIf { items.isNotEmpty() && it < total }
}
