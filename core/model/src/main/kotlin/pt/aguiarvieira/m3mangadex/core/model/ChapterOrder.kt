package pt.aguiarvieira.m3mangadex.core.model

/**
 * Reading order. MangaDex chapter numbers are free-form strings ("10", "10.5", "Extra"); they're
 * compared numerically where they parse, unnumbered ones first, ties by text then release time.
 */
val ChapterOrder: Comparator<Chapter> =
    compareBy<Chapter> { it.number.numericKey() }.thenBy { it.number }.thenBy { it.readableAt }

internal fun String?.numericKey(): Double = this?.toDoubleOrNull() ?: Double.NEGATIVE_INFINITY

/** The chapters around one being read, for "previous" and "next". */
data class ChapterNeighbors(
    val previous: Chapter?,
    val next: Chapter?,
)

/**
 * Previous and next chapters by number, skipping other uploads of the same number. Where several
 * groups released the neighbouring number, the current chapter's group is preferred, so a reader
 * following one translation stays with it.
 */
fun neighbors(
    chapters: List<Chapter>,
    current: Chapter,
): ChapterNeighbors {
    val key = current.number.numericKey()
    val groups = current.groups.map { it.id }.toSet()

    fun pick(candidates: List<Chapter>): Chapter? {
        val number = candidates.firstOrNull()?.number?.numericKey() ?: return null
        val same = candidates.takeWhile { it.number.numericKey() == number }
        return same.firstOrNull { c -> c.groups.any { it.id in groups } } ?: same.first()
    }
    val sorted = chapters.sortedWith(ChapterOrder)
    // Unnumbered chapters (oneshots) have no neighbours by number.
    if (current.number?.toDoubleOrNull() == null) return ChapterNeighbors(null, null)
    val next = pick(sorted.filter { it.number.numericKey() > key })
    val previous =
        pick(sorted.filter { it.number.numericKey() < key && it.number?.toDoubleOrNull() != null }.asReversed())
    return ChapterNeighbors(previous, next)
}
