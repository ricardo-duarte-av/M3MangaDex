package pt.aguiarvieira.m3mangadex.core.data

import pt.aguiarvieira.m3mangadex.core.model.MangaFilter
import pt.aguiarvieira.m3mangadex.core.model.MangaOrder
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/** The rows on the Browse home, each a canned `/manga` query ("See all" opens it in search). */
enum class BrowseSection {
    /** Most-followed titles added in the last month (MangaDex's own "Popular New Titles"). */
    PopularNew,
    LatestUpdates,
    RecentlyAdded,
    MostFollowed,
    TopRated,
    ;

    fun filter(
        languages: Collection<String>,
        now: Instant = Instant.now(),
    ): MangaFilter {
        val base = MangaFilter(availableLanguages = languages.toSet())
        return when (this) {
            PopularNew -> base.copy(order = MangaOrder.Follows, createdAtSince = monthBefore(now))
            LatestUpdates -> base.copy(order = MangaOrder.LatestUpload)
            RecentlyAdded -> base.copy(order = MangaOrder.Created)
            MostFollowed -> base.copy(order = MangaOrder.Follows)
            TopRated -> base.copy(order = MangaOrder.Rating)
        }
    }

    private companion object {
        const val POPULAR_WINDOW_DAYS = 30L

        // MangaDex wants a bare local date-time (no offset), interpreted as UTC.
        val apiDateTime: DateTimeFormatter =
            DateTimeFormatter
                .ofPattern(
                    "yyyy-MM-dd'T'HH:mm:ss"
                ).withZone(ZoneOffset.UTC)

        fun monthBefore(now: Instant): String =
            apiDateTime.format(now.minus(POPULAR_WINDOW_DAYS, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS))
    }
}
