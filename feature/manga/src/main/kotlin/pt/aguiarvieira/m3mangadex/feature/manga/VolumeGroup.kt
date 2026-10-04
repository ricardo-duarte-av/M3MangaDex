package pt.aguiarvieira.m3mangadex.feature.manga

import pt.aguiarvieira.m3mangadex.core.model.Chapter

/** Chapters of one volume ([volume] null: not yet collected in a volume). */
data class VolumeGroup(
    val volume: String?,
    val chapters: List<Chapter>,
)

/**
 * Groups chapters by volume, newest volume first, unvolumed chapters (usually the newest ones)
 * on top. Within a volume, chapters run newest first; MangaDex's numbers are free-form strings,
 * so they're compared numerically where they parse and textually otherwise.
 */
fun groupByVolume(chapters: List<Chapter>): List<VolumeGroup> =
    chapters
        .groupBy { it.volume }
        .map { (volume, items) -> VolumeGroup(volume, items.sortedWith(chapterOrder.reversed())) }
        .sortedWith(
            compareByDescending<VolumeGroup> {
                it.volume == null
            }.thenByDescending { it.volume.sortKey() }.thenByDescending { it.volume }
        )

private val chapterOrder: Comparator<Chapter> =
    compareBy<Chapter> { it.number.sortKey() }.thenBy { it.number }.thenBy { it.readableAt }

private fun String?.sortKey(): Double = this?.toDoubleOrNull() ?: Double.NEGATIVE_INFINITY

/**
 * The chapter to start with: the lowest-numbered one hosted on MangaDex, else (official releases
 * only, like Solo Leveling's) the lowest-numbered one on the publisher's site.
 */
fun firstReadable(chapters: List<Chapter>): Chapter? =
    chapters.filterNot { it.isExternal }.minWithOrNull(chapterOrder) ?: chapters.minWithOrNull(chapterOrder)
