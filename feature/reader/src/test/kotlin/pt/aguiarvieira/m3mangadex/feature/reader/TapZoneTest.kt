package pt.aguiarvieira.m3mangadex.feature.reader

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import org.junit.Assert.assertEquals
import org.junit.Test
import pt.aguiarvieira.m3mangadex.core.model.ReaderMode

class TapZoneTest {
    private val size = IntSize(900, 1800)

    @Test
    fun `left to right reads left as back and right as forward`() {
        assertEquals(TapZone.Back, tapZone(Offset(100f, 900f), size, ReaderMode.LeftToRight))
        assertEquals(TapZone.Forward, tapZone(Offset(800f, 900f), size, ReaderMode.LeftToRight))
        assertEquals(TapZone.Middle, tapZone(Offset(450f, 900f), size, ReaderMode.LeftToRight))
    }

    @Test
    fun `right to left mirrors the sides`() {
        assertEquals(TapZone.Forward, tapZone(Offset(100f, 900f), size, ReaderMode.RightToLeft))
        assertEquals(TapZone.Back, tapZone(Offset(800f, 900f), size, ReaderMode.RightToLeft))
    }

    @Test
    fun `vertical modes use the top and bottom`() {
        assertEquals(TapZone.Back, tapZone(Offset(100f, 100f), size, ReaderMode.Webtoon))
        assertEquals(TapZone.Forward, tapZone(Offset(100f, 1700f), size, ReaderMode.Vertical))
        assertEquals(TapZone.Middle, tapZone(Offset(100f, 900f), size, ReaderMode.Webtoon))
    }

    @Test
    fun `before layout every tap is a middle tap`() {
        assertEquals(TapZone.Middle, tapZone(Offset(1f, 1f), IntSize.Zero, ReaderMode.LeftToRight))
    }
}
