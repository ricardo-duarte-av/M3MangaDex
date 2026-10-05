package pt.aguiarvieira.m3mangadex.core.model

/** A crop in pixels: the region to keep, right/bottom exclusive. */
data class CropRect(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
) {
    val width: Int get() = right - left
    val height: Int get() = bottom - top
}

/**
 * Finds the solid black or white margins some scans carry around the artwork (a frame of 60–70 px on
 * an 870 px page is common), so the reader can trim them and let the art fill the screen.
 *
 * Each side is trimmed on its own, but only through lines that are almost entirely margin colour
 * (allowing for JPEG noise), only when the margin is worth it, and never so deep that a dark or
 * blank page loses its content: an axis whose two margins would remove too much is left alone.
 */
object Borders {
    private const val DARK_MAX = 50
    private const val LIGHT_MIN = 205

    /** A line still counts as margin with up to this share of off-colour pixels (noise, dust). */
    private const val NOISE_TOLERANCE = 0.02

    /** Margins thinner than this share of the dimension aren't worth a crop. */
    private const val MIN_MARGIN = 0.01

    /** Never remove more than this share of an axis in total. */
    private const val MAX_AXIS_CROP = 0.3

    /** Sample every Nth pixel along a line: plenty for solid margins, and fast on big pages. */
    private const val STRIDE = 4

    /** Smaller images aren't scans worth cropping. */
    private const val MIN_SIZE = STRIDE * 4

    /** The region to keep of an ARGB [pixels] image, or null when there's nothing to trim. */
    fun detect(
        pixels: IntArray,
        width: Int,
        height: Int,
    ): CropRect? {
        if (width < MIN_SIZE || height < MIN_SIZE) return null
        val margin = marginColour(pixels, width, height) ?: return null
        val (left, right) = margins(width) { x -> lineIsMargin(height, margin) { y -> pixels[y * width + x] } }
        val (top, bottom) = margins(height) { y -> lineIsMargin(width, margin) { x -> pixels[y * width + x] } }
        val (cropLeft, cropRight) = axis(left, right, width)
        val (cropTop, cropBottom) = axis(top, bottom, height)
        if (cropLeft + cropRight + cropTop + cropBottom == 0) return null
        return CropRect(cropLeft, cropTop, width - cropRight, height - cropBottom)
    }

    /** How many lines from each end of an axis of [size] lines are margin, up to its middle. */
    private inline fun margins(
        size: Int,
        isMargin: (Int) -> Boolean,
    ): Pair<Int, Int> {
        var start = 0
        while (start < size / 2 && isMargin(start)) start++
        var end = 0
        while (end < size / 2 && isMargin(size - 1 - end)) end++
        return start to end
    }

    /** Drops margins too thin to matter; drops both if together they'd eat too much of the page. */
    private fun axis(
        start: Int,
        end: Int,
        size: Int,
    ): Pair<Int, Int> {
        if (start + end > size * MAX_AXIS_CROP) return 0 to 0
        val min = (size * MIN_MARGIN).toInt().coerceAtLeast(1)
        return (if (start >= min) start else 0) to (if (end >= min) end else 0)
    }

    private enum class Margin { Dark, Light }

    /** The margin colour, when the four corners agree on one (black or white). */
    private fun marginColour(
        pixels: IntArray,
        width: Int,
        height: Int,
    ): Margin? {
        val corners =
            listOf(pixels[0], pixels[width - 1], pixels[(height - 1) * width], pixels[height * width - 1]).map(::luma)
        return when {
            corners.all { it <= DARK_MAX } -> Margin.Dark
            corners.all { it >= LIGHT_MIN } -> Margin.Light
            else -> null
        }
    }

    private inline fun lineIsMargin(
        length: Int,
        margin: Margin,
        pixelAt: (Int) -> Int,
    ): Boolean {
        var samples = 0
        var off = 0
        var i = 0
        while (i < length) {
            val luma = luma(pixelAt(i))
            val matches = if (margin == Margin.Dark) luma <= DARK_MAX else luma >= LIGHT_MIN
            if (!matches) off++
            samples++
            i += STRIDE
        }
        return off <= samples * NOISE_TOLERANCE
    }

    @Suppress("MagicNumber") // Rec. 601 luma weights.
    private fun luma(argb: Int): Int {
        val r = (argb shr 16) and 0xFF
        val g = (argb shr 8) and 0xFF
        val b = argb and 0xFF
        return (r * 299 + g * 587 + b * 114) / 1000
    }
}
