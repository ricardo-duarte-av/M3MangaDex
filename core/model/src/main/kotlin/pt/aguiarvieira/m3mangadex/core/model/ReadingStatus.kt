package pt.aguiarvieira.m3mangadex.core.model

/** Where a manga sits in the user's MangaDex library. */
enum class ReadingStatus(
    val apiValue: String,
) {
    Reading("reading"),
    PlanToRead("plan_to_read"),
    OnHold("on_hold"),
    ReReading("re_reading"),
    Completed("completed"),
    Dropped("dropped"),
    ;

    companion object {
        fun of(value: String?) = entries.firstOrNull { it.apiValue == value }
    }
}

/** A chapter from the user's follows feed, with the manga it belongs to. */
data class FeedEntry(
    val chapter: Chapter,
    val manga: Manga?,
)
