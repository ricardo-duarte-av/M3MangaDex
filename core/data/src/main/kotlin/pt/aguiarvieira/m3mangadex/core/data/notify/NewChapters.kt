package pt.aguiarvieira.m3mangadex.core.data.notify

import pt.aguiarvieira.m3mangadex.core.model.FeedEntry

/** New chapters of one followed manga, newest first. */
data class NewChapters(
    val mangaId: String,
    val entries: List<FeedEntry>,
)

/**
 * The follows-feed entries released after [since] (epoch millis), grouped by manga, most recently
 * updated manga first. Several uploads of one chapter number (other groups, other languages) count
 * once, so "3 new chapters" means three chapters.
 */
fun newChapters(
    feed: List<FeedEntry>,
    since: Long,
): List<NewChapters> =
    feed
        .filter { (it.chapter.readableAt?.toEpochMilli() ?: 0L) > since && it.chapter.mangaId != null }
        .groupBy { it.chapter.mangaId!! }
        .map { (mangaId, entries) ->
            NewChapters(
                mangaId,
                entries.sortedByDescending { it.chapter.readableAt }.distinctBy {
                    it.chapter.number
                        ?: it.chapter.id
                }
            )
        }.sortedByDescending { group ->
            group.entries
                .first()
                .chapter.readableAt
        }
