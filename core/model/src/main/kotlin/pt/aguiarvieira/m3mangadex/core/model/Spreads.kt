package pt.aguiarvieira.m3mangadex.core.model

/**
 * Pairs pages into two-page spreads for wide screens, the way a printed volume opens: the first
 * page (usually a cover or title page) stands alone, then pages go in pairs. A page that is itself
 * a spread (wider than tall, in [wide]) always stands alone, and the pairing restarts after it so
 * the facing pages stay facing.
 *
 * Each spread lists page indices in reading order; the screen lays them out right-to-left for
 * [ReaderMode.RightToLeft].
 */
fun spreads(
    pageCount: Int,
    wide: Set<Int>,
): List<List<Int>> {
    val result = mutableListOf<List<Int>>()
    var index = 0
    while (index < pageCount) {
        val pairable = index != 0 && index !in wide && index + 1 < pageCount && index + 1 !in wide
        if (pairable) {
            result += listOf(index, index + 1)
            index += 2
        } else {
            result += listOf(index)
            index += 1
        }
    }
    return result
}

/** The spread showing [page], for keeping the reader's place when the pairing changes. */
fun List<List<Int>>.spreadOf(page: Int): Int = indexOfFirst { page in it }.coerceAtLeast(0)
