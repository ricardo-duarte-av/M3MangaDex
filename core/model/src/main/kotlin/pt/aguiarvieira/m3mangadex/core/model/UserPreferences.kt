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
    /** How paged modes scale a page to the screen. */
    val pageFit: PageFit = PageFit.Auto,
    /** Trim solid black or white margins scanned around the artwork. */
    val cropBorders: Boolean = true,
    /** Tint screens with colours picked from the manga's cover. */
    val coverTheming: Boolean = true,
    /** Notify about new chapters of followed manga (needs an account). */
    val newChapterNotifications: Boolean = false,
)

/** How a page (or spread) is scaled in the paged reader modes. */
enum class PageFit {
    /** The whole page is visible: [Screen] in portrait, [Width] in landscape. */
    Auto,

    /** The whole page is visible, letterboxed as needed. */
    Screen,

    /** The page fills the width; taller pages scroll down before the page turns. */
    Width,

    /** The page fills the height. */
    Height,
}
