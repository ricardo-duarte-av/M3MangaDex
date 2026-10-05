package pt.aguiarvieira.m3mangadex.core.designsystem.theme

import android.graphics.Bitmap
import android.util.LruCache
import androidx.compose.ui.graphics.Color
import com.materialkolor.quantize.QuantizerCelebi
import com.materialkolor.score.Score

/**
 * "What colour is this cover?", answered the way Android picks a wallpaper's seed (Monet: Celebi
 * quantization, then [Score]), so a cover-themed screen looks like the system would theme it.
 * Ported from jellymusic's AlbumSeed, where a saturation-first picker (Palette) proved to chase
 * small vivid accents instead of the cover's dominant mass.
 */
object CoverSeed {
    /** Covers are quantized at this size: enough colour detail, cheap enough to run per screen. */
    private const val BITMAP_PX = 128

    /** Palette size handed to [Score]; matches Monet's bucket count. */
    private const val MAX_COLORS = 128

    private const val CACHE_SIZE = 64

    /** Seeds by cover URL: reopening a manga re-themes instantly. 0 marks "no usable seed". */
    private val cache = LruCache<String, Int>(CACHE_SIZE)

    fun cached(url: String): Color? = cache.get(url)?.takeIf { it != 0 }?.let(::Color)

    fun isCached(url: String): Boolean = cache.get(url) != null

    /**
     * The seed for [bitmap], or null when no colour qualifies (a greyscale or blank cover): the
     * screen then keeps the app's own theme instead of inventing a hue.
     */
    fun from(
        url: String,
        bitmap: Bitmap,
    ): Color? {
        val scaled = Bitmap.createScaledBitmap(bitmap, BITMAP_PX, BITMAP_PX, true)
        val pixels = IntArray(scaled.width * scaled.height)
        scaled.getPixels(pixels, 0, scaled.width, 0, 0, scaled.width, scaled.height)
        if (scaled !== bitmap) scaled.recycle()
        val quantized = QuantizerCelebi.quantize(pixels, MAX_COLORS)
        // desired = 1: only the winner. No fallback, so an unscoreable cover reports "no seed"
        // rather than theming everything Monet's default blue.
        val argb = Score.score(quantized, 1, null, true).firstOrNull()
        cache.put(url, argb ?: 0)
        return argb?.let(::Color)
    }
}
