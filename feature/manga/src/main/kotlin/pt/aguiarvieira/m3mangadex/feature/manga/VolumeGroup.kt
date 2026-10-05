package pt.aguiarvieira.m3mangadex.feature.manga

import pt.aguiarvieira.m3mangadex.core.model.Chapter
import pt.aguiarvieira.m3mangadex.core.model.ChapterOrder

/** Chapters of one volume ([volume] null: not yet collected in a volume). */
data class VolumeGroup(
    val volume: String?,
    val chapters: List<Chapter>,
)

/**
 * Groups chapters by volume, newest volume first, unvolumed chapters (usually the newest ones)
 * on top. Within a volume, chapters run newest first ([ChapterOrder], reversed).
 */
fun groupByVolume(chapters: List<Chapter>): List<VolumeGroup> =
    chapters
        .groupBy { it.volume }
        .map { (volume, items) -> VolumeGroup(volume, items.sortedWith(ChapterOrder.reversed())) }
        .sortedWith(volumeOrder)

private val volumeOrder: Comparator<VolumeGroup> =
    compareByDescending<VolumeGroup> { it.volume == null }
        .thenByDescending { it.volume?.toDoubleOrNull() ?: Double.NEGATIVE_INFINITY }
        .thenByDescending { it.volume }

/**
 * The chapter to start with: the lowest-numbered one hosted on MangaDex, else (official releases
 * only, like Solo Leveling's) the lowest-numbered one on the publisher's site.
 */
fun firstReadable(chapters: List<Chapter>): Chapter? =
    chapters.filterNot { it.isExternal }.minWithOrNull(ChapterOrder) ?: chapters.minWithOrNull(ChapterOrder)
