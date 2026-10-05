package pt.aguiarvieira.m3mangadex.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BordersTest {
    private val black = 0xFF000000.toInt()
    private val white = 0xFFFFFFFF.toInt()
    private val grey = 0xFF808080.toInt()

    /** A [width]×[height] page of [art], framed by [margin] pixels of [frame] on each listed side. */
    private fun page(
        width: Int = 870,
        height: Int = 1236,
        frame: Int = black,
        art: Int = grey,
        left: Int = 0,
        top: Int = 0,
        right: Int = 0,
        bottom: Int = 0,
    ) = IntArray(width * height) { i ->
        val x = i % width
        val y = i / width
        val inArt = x in left until width - right && y in top until height - bottom
        if (inArt) art else frame
    }

    @Test
    fun `trims a black frame like the Unwanted Undead scans`() {
        val pixels = page(left = 67, top = 68, right = 67, bottom = 68)
        assertEquals(CropRect(67, 68, 803, 1168), Borders.detect(pixels, 870, 1236))
    }

    @Test
    fun `trims white margins too`() {
        val pixels = page(frame = white, left = 40, right = 40, top = 30, bottom = 30)
        assertEquals(CropRect(40, 30, 830, 1206), Borders.detect(pixels, 870, 1236))
    }

    @Test
    fun `leaves full-bleed pages alone`() {
        assertNull(Borders.detect(page(), 870, 1236))
    }

    @Test
    fun `ignores slivers thinner than one percent`() {
        assertNull(Borders.detect(page(left = 3, top = 3, right = 3, bottom = 3), 870, 1236))
    }

    @Test
    fun `keeps a mostly dark page from being eaten`() {
        // 40% black either side: too much to be a scanning margin.
        assertNull(Borders.detect(page(left = 350, right = 350, top = 500, bottom = 500), 870, 1236))
    }

    @Test
    fun `tolerates JPEG noise in the margin`() {
        val pixels = page(left = 67, top = 68, right = 67, bottom = 68)
        // A speck of dust in the left margin.
        pixels[600 * 870 + 10] = white
        assertEquals(CropRect(67, 68, 803, 1168), Borders.detect(pixels, 870, 1236))
    }
}
