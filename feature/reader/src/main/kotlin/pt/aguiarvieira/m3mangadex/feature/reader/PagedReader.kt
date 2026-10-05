package pt.aguiarvieira.m3mangadex.feature.reader

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import me.saket.telephoto.zoomable.ZoomSpec
import me.saket.telephoto.zoomable.ZoomableState
import me.saket.telephoto.zoomable.rememberZoomableState
import pt.aguiarvieira.m3mangadex.core.designsystem.component.Loading
import pt.aguiarvieira.m3mangadex.core.model.PageFit
import pt.aguiarvieira.m3mangadex.core.model.ReaderMode
import pt.aguiarvieira.m3mangadex.core.model.spreadOf
import pt.aguiarvieira.m3mangadex.core.model.spreads

/**
 * One page (or a two-page spread) at a time, in [mode]'s direction, scaled by [fit]. Pinch or
 * double-tap to zoom; a zoomed (or taller-than-the-screen) page pans until its edge, then the pager
 * takes over. Taps on the outer thirds go forward or back (mirrored for right-to-left), scrolling
 * through a tall page before turning it; the middle toggles the chrome. One extra page after the
 * last holds [endContent].
 */
@Composable
internal fun PagedReader(
    urls: List<String>,
    generation: Int,
    mode: ReaderMode,
    spreadsEnabled: Boolean,
    fit: PageFit,
    cropBorders: Boolean,
    startPage: Int,
    failedPages: Set<Int>,
    commands: Flow<ReaderCommand>,
    onPageShow: (Int) -> Unit,
    onEndShow: () -> Unit,
    onToggleChrome: () -> Unit,
    onPageFail: (Int) -> Unit,
    onRetryPage: (Int) -> Unit,
    endContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val window = windowShape()
    val wide = remember(urls.size, cropBorders) { mutableStateSetOf<Int>() }
    val layout = rememberLayout(urls.size, spreadsEnabled && mode != ReaderMode.Vertical && window.wide, wide.toSet())
    val resolvedFit = if (fit == PageFit.Auto) (if (window.landscape) PageFit.Width else PageFit.Screen) else fit
    // The first page of the spread on screen: survives the pairing changing as page sizes arrive.
    var anchor by rememberSaveable { mutableIntStateOf(startPage) }
    val pager = rememberPagerState(initialPage = layout.spreadOf(startPage)) { layout.size + 1 }
    val currentLayout by rememberUpdatedState(layout)
    val pageShown by rememberUpdatedState(onPageShow)
    val endShown by rememberUpdatedState(onEndShow)
    // Each visible spread's zoom state, so forward/back can scroll within a tall page first.
    val zoomStates = remember { mutableStateMapOf<Int, ZoomableState>() }
    var size by remember { mutableStateOf(IntSize.Zero) }

    val step: suspend (Int) -> Unit = { delta ->
        val scrolled = zoomStates[pager.currentPage]?.scrollWithin(delta, size.height * SCREEN_STEP) == true
        if (!scrolled) pager.animateScrollToPage((pager.currentPage + delta).coerceIn(0, currentLayout.size))
    }

    LaunchedEffect(layout) {
        val target = layout.spreadOf(anchor)
        if (pager.currentPage < layout.size && pager.currentPage != target) pager.scrollToPage(target)
    }
    LaunchedEffect(pager) {
        snapshotFlow { pager.settledPage }.collect { index ->
            val spread = currentLayout.getOrNull(index)
            if (spread == null) {
                endShown()
            } else {
                anchor = spread.first()
                pageShown(spread.last())
            }
        }
    }
    LaunchedEffect(pager, commands) {
        commands.collect { command ->
            when (command) {
                is ReaderCommand.Jump -> pager.animateScrollToPage(currentLayout.spreadOf(command.page))
                is ReaderCommand.Step -> step(command.delta)
            }
        }
    }
    PreloadPages(
        urls = urls,
        generation = generation,
        current = anchor,
        cropBorders = cropBorders,
        onSize = { page, width, height -> if (width > height) wide.add(page) },
        onFail = onPageFail,
    )

    val scope = rememberCoroutineScope()
    val onTap: (Offset) -> Unit = { offset ->
        when (tapZone(offset, size, mode)) {
            TapZone.Back -> scope.launch { step(-1) }
            TapZone.Forward -> scope.launch { step(1) }
            TapZone.Middle -> onToggleChrome()
        }
    }
    val page: @Composable (Int) -> Unit = { index ->
        val spread = layout.getOrNull(index)
        if (spread == null) {
            endContent()
        } else {
            key(generation, cropBorders) {
                val state = rememberZoomableState(zoomSpec = ZoomSpec(maxZoomFactor = MAX_ZOOM))
                DisposableEffect(index, state) {
                    zoomStates[index] = state
                    onDispose { zoomStates.remove(index) }
                }
                SpreadContent(
                    spread,
                    urls,
                    mode,
                    resolvedFit,
                    cropBorders,
                    failedPages,
                    state,
                    onTap,
                    onPageFail,
                    onRetryPage
                )
            }
        }
    }
    Box(modifier.fillMaxSize().onSizeChanged { size = it }) {
        Pager(pager, mode, page)
    }
}

/**
 * Scrolls a page taller than the screen one step down ([delta] > 0) or up, if there's more of it
 * that way. False when the page is already showing that edge, and it's time to turn the page.
 */
private suspend fun ZoomableState.scrollWithin(
    delta: Int,
    step: Float,
): Boolean {
    val overflow = overflow()
    val remaining = if (delta > 0) overflow.below else overflow.above
    if (remaining <= 1f) return false
    val distance = minOf(step, remaining)
    panBy(Offset(0f, if (delta > 0) -distance else distance))
    return true
}

// The two pagers are alternatives (the mode decides which exists), never both on screen; a mode
// change replaces the reader's content anyway, so there is no slot state to preserve.
@Suppress("ContentSlotReused")
@Composable
private fun Pager(
    state: PagerState,
    mode: ReaderMode,
    page: @Composable (Int) -> Unit,
) {
    if (mode == ReaderMode.Vertical) {
        VerticalPager(state = state, beyondViewportPageCount = 1, modifier = Modifier.fillMaxSize()) { page(it) }
    } else {
        HorizontalPager(
            state = state,
            beyondViewportPageCount = 1,
            // Right-to-left: page 1 on the right, the next one slides in from the left.
            reverseLayout = mode == ReaderMode.RightToLeft,
            modifier = Modifier.fillMaxSize(),
        ) { page(it) }
    }
}

@Composable
private fun SpreadContent(
    spread: List<Int>,
    urls: List<String>,
    mode: ReaderMode,
    fit: PageFit,
    cropBorders: Boolean,
    failedPages: Set<Int>,
    state: ZoomableState,
    onTap: (Offset) -> Unit,
    onPageFail: (Int) -> Unit,
    onRetryPage: (Int) -> Unit,
) {
    val failed = spread.firstOrNull { it in failedPages }
    if (failed != null) {
        PageFailed(page = failed, onRetry = { onRetryPage(failed) }, modifier = Modifier.fillMaxSize())
        return
    }
    // Facing pages share one picture; in right-to-left order the first page sits on the right.
    val ordered = if (mode == ReaderMode.RightToLeft) spread.reversed() else spread
    val painters = ordered.map { page -> pagePainter(urls[page], cropBorders) { onPageFail(page) } }
    val painter =
        when (painters.size) {
            1 -> painters.single()
            else -> remember(painters[0], painters[1]) { SpreadPainter(painters[0], painters[1]) }
        }
    val states = painters.map { it.state.collectAsStateWithLifecycle().value }
    val loaded = states.all { it is AsyncImagePainter.State.Success }
    val description = spread.map { stringResource(R.string.reader_page, it + 1) }.joinToString()
    Box(Modifier.fillMaxSize()) {
        ZoomablePage(
            painter = painter,
            fit = fit,
            state = state,
            contentDescription = description,
            onTap = onTap,
        )
        if (!loaded) Loading(Modifier.align(Alignment.Center))
    }
}

@Composable
private fun pagePainter(
    url: String,
    cropBorders: Boolean,
    onFail: () -> Unit,
): AsyncImagePainter {
    val context = LocalContext.current
    val request = remember(url, cropBorders) { pageRequest(context, url, cropBorders).build() }
    return rememberAsyncImagePainter(model = request, onError = { onFail() })
}

@Composable
private fun rememberLayout(
    pageCount: Int,
    useSpreads: Boolean,
    wide: Set<Int>,
): List<List<Int>> =
    remember(pageCount, useSpreads, wide) {
        if (useSpreads) spreads(pageCount, wide) else List(pageCount) { listOf(it) }
    }

private data class WindowShape(
    val landscape: Boolean,
    /** Landscape and wide enough for two readable pages side by side. */
    val wide: Boolean,
)

@Composable
private fun windowShape(): WindowShape {
    val size = LocalWindowInfo.current.containerSize
    val widthDp = with(LocalDensity.current) { size.width.toDp() }
    val landscape = size.width > size.height
    return WindowShape(landscape, landscape && widthDp >= MIN_SPREAD_WIDTH)
}

private val MIN_SPREAD_WIDTH = 600.dp
private const val MAX_ZOOM = 3f

/** How far one forward/back step scrolls within a page taller than the screen. */
private const val SCREEN_STEP = 0.85f

internal enum class TapZone { Back, Middle, Forward }

/** Outer thirds turn pages (mirrored for right-to-left), the middle third is for the chrome. */
internal fun tapZone(
    offset: Offset,
    size: IntSize,
    mode: ReaderMode,
): TapZone {
    if (size == IntSize.Zero) return TapZone.Middle
    val fraction =
        if (mode == ReaderMode.Vertical ||
            mode == ReaderMode.Webtoon
        ) {
            offset.y / size.height
        } else {
            offset.x / size.width
        }
    val zone =
        when {
            fraction < EDGE_FRACTION -> TapZone.Back
            fraction > 1 - EDGE_FRACTION -> TapZone.Forward
            else -> TapZone.Middle
        }
    return if (mode == ReaderMode.RightToLeft && zone != TapZone.Middle) {
        if (zone == TapZone.Back) TapZone.Forward else TapZone.Back
    } else {
        zone
    }
}

private const val EDGE_FRACTION = 1f / 3
