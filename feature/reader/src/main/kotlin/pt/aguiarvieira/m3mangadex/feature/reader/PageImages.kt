package pt.aguiarvieira.m3mangadex.feature.reader

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import coil3.request.ImageRequest
import coil3.request.transformations
import coil3.transform.Transformation
import pt.aguiarvieira.m3mangadex.core.model.Borders
import coil3.size.Size as CoilSize

/**
 * The one request shape for a page, shared by what's on screen and the preloader so they hit the
 * same cache entry. Pages load at full resolution (zooming in should show real detail), cropped
 * when [cropBorders] is on.
 */
internal fun pageRequest(
    context: Context,
    url: String,
    cropBorders: Boolean,
): ImageRequest.Builder =
    ImageRequest
        .Builder(context)
        .data(url)
        .size(CoilSize.ORIGINAL)
        .apply { if (cropBorders) transformations(CropBordersTransformation) }

/** Trims scanned black or white margins ([Borders]); pages without any pass through untouched. */
internal object CropBordersTransformation : Transformation() {
    override val cacheKey: String = "crop-borders-v1"

    override suspend fun transform(
        input: Bitmap,
        size: CoilSize,
    ): Bitmap {
        val pixels = IntArray(input.width * input.height)
        input.getPixels(pixels, 0, input.width, 0, 0, input.width, input.height)
        val crop = Borders.detect(pixels, input.width, input.height) ?: return input
        return Bitmap.createBitmap(input, crop.left, crop.top, crop.width, crop.height)
    }
}

/**
 * Two pages side by side as one picture, so a spread zooms, pans and fits exactly like a single
 * page. [first] is drawn on the left (the caller orders them for right-to-left reading). Pages of
 * different heights are scaled to the taller one, so they meet cleanly at the gutter.
 */
internal class SpreadPainter(
    private val first: Painter,
    private val second: Painter,
) : Painter() {
    override val intrinsicSize: Size
        get() {
            val a = first.intrinsicSize
            val b = second.intrinsicSize
            if (!a.isUsable() || !b.isUsable()) return Size.Unspecified
            val height = maxOf(a.height, b.height)
            return Size(a.width * height / a.height + b.width * height / b.height, height)
        }

    override fun DrawScope.onDraw() {
        val a = first.intrinsicSize
        val b = second.intrinsicSize
        if (!a.isUsable() || !b.isUsable()) return
        val scale = size.height / maxOf(a.height, b.height)
        val firstSize = Size(a.width * maxOf(a.height, b.height) / a.height * scale, size.height)
        val secondSize = Size(size.width - firstSize.width, size.height)
        with(first) { draw(firstSize) }
        translate(left = firstSize.width) { with(second) { draw(secondSize) } }
    }

    private fun Size.isUsable() = this != Size.Unspecified && width > 0 && height > 0
}
