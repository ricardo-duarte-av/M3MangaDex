package pt.aguiarvieira.m3mangadex.core.model

/** How the reader lays out a chapter's pages. */
enum class ReaderMode {
    /** One page at a time, swiping right-to-left like a western comic. */
    LeftToRight,

    /** One page at a time, swiping left-to-right: Japanese manga's reading direction. */
    RightToLeft,

    /** One page at a time, swiping up. */
    Vertical,

    /** A continuous vertical strip with no gaps: Korean and Chinese webtoons. */
    Webtoon,
    ;

    val isPaged: Boolean get() = this != Webtoon

    companion object {
        /** MangaDex's "Long Strip" format tag. */
        const val LONG_STRIP_TAG = "3e2b8dae-350e-4ab8-a8ce-016e844b9f0d"

        /** Japanese reads right to left, long strips scroll, the rest reads left to right. */
        fun defaultFor(manga: Manga): ReaderMode =
            when {
                manga.tags.any { it.id == LONG_STRIP_TAG } -> Webtoon
                manga.originalLanguage in WEBTOON_LANGUAGES -> Webtoon
                manga.originalLanguage == "ja" -> RightToLeft
                else -> LeftToRight
            }

        private val WEBTOON_LANGUAGES = setOf("ko", "zh", "zh-hk")
    }
}
