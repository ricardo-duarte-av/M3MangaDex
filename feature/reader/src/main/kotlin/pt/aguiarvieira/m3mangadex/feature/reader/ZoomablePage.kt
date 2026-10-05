package pt.aguiarvieira.m3mangadex.feature.reader

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import me.saket.telephoto.ExperimentalTelephotoApi
import me.saket.telephoto.zoomable.Viewport
import me.saket.telephoto.zoomable.ZoomableContentLocation
import me.saket.telephoto.zoomable.ZoomableState
import me.saket.telephoto.zoomable.spatial.CoordinateSpace
import me.saket.telephoto.zoomable.zoomable
import pt.aguiarvieira.m3mangadex.core.model.PageFit

/**
 * A page (or a [SpreadPainter]) under telephoto's zoom: pinch and double-tap to zoom, pan when
 * zoomed or when [fit] makes the page bigger than the screen. Rendered from a full-resolution
 * bitmap rather than telephoto's sub-sampling, so the page can be cropped first.
 */
@Composable
internal fun ZoomablePage(
    painter: Painter?,
    fit: PageFit,
    state: ZoomableState,
    contentDescription: String?,
    onTap: (Offset) -> Unit,
    modifier: Modifier = Modifier,
) {
    state.contentScale =
        when (fit) {
            PageFit.Width -> ContentScale.FillWidth
            PageFit.Height -> ContentScale.FillHeight
            PageFit.Screen, PageFit.Auto -> ContentScale.Fit
        }
    // Fit to width starts at the top of the page, like opening a book.
    state.contentAlignment = if (fit == PageFit.Width) Alignment.TopCenter else Alignment.Center
    state.setContentLocation(ZoomableContentLocation.scaledInsideAndCenterAligned(painter?.intrinsicSize))
    Image(
        painter = painter ?: Empty,
        contentDescription = contentDescription,
        contentScale = ContentScale.Inside,
        alignment = Alignment.Center,
        modifier = modifier.fillMaxSize().zoomable(state, onClick = onTap),
    )
}

/** Pixels of the page still hidden below (positive) or above (negative edge) the screen. */
@OptIn(ExperimentalTelephotoApi::class)
internal fun ZoomableState.overflow(): PageOverflow {
    with(coordinateSystem) {
        val bounds = contentBounds(clipToViewport = false).rectIn(CoordinateSpace.Viewport)
        val viewport = viewportSize
        if (viewport.height <= 0f || bounds.isEmpty) return PageOverflow(0f, 0f)
        return PageOverflow(
            above = (-bounds.top).coerceAtLeast(0f),
            below = (bounds.bottom - viewport.height).coerceAtLeast(0f),
        )
    }
}

internal data class PageOverflow(
    val above: Float,
    val below: Float,
)

private val Empty = ColorPainter(Color.Transparent)
