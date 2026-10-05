package pt.aguiarvieira.m3mangadex.feature.reader

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import me.saket.telephoto.zoomable.coil3.ZoomableAsyncImage
import me.saket.telephoto.zoomable.rememberZoomableImageState
import me.saket.telephoto.zoomable.rememberZoomableState
import me.saket.telephoto.zoomable.zoomable
import pt.aguiarvieira.m3mangadex.core.designsystem.component.Loading
import pt.aguiarvieira.m3mangadex.core.model.ReaderMode
import pt.aguiarvieira.m3mangadex.core.model.spreadOf
import pt.aguiarvieira.m3mangadex.core.model.spreads

/**
 * One page (or a two-page spread) at a time, in [mode]'s direction. Pinch or double-tap to zoom;
 * a zoomed page pans until its edge, then the pager takes over. Taps on the outer thirds turn the
 * page (respecting reading direction), the middle toggles the chrome. One extra page after the
 * last holds [endContent].
 */
@Composable
internal fun PagedReader(
    urls: List<String>,
    generation: Int,
    mode: ReaderMode,
    spreadsEnabled: Boolean,
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
    val wide = remember(urls.size) { mutableStateSetOf<Int>() }
    val layout =
        rememberLayout(urls.size, spreadsEnabled && mode != ReaderMode.Vertical && isWideWindow(), wide.toSet())
    // The first page of the spread on screen: survives the pairing changing as page sizes arrive.
    var anchor by rememberSaveable { mutableIntStateOf(startPage) }
    val pager = rememberPagerState(initialPage = layout.spreadOf(startPage)) { layout.size + 1 }
    val currentLayout by rememberUpdatedState(layout)
    val pageShown by rememberUpdatedState(onPageShow)
    val endShown by rememberUpdatedState(onEndShow)

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
                is ReaderCommand.Jump -> {
                    pager.animateScrollToPage(currentLayout.spreadOf(command.page))
                }

                is ReaderCommand.Step -> {
                    pager.animateScrollToPage(
                        (pager.currentPage + command.delta).coerceIn(0, currentLayout.size)
                    )
                }
            }
        }
    }
    PreloadPages(
        urls = urls,
        generation = generation,
        current = anchor,
        onSize = { page, width, height -> if (width > height) wide.add(page) },
        onFail = onPageFail,
    )

    val scope = rememberCoroutineScope()
    var size by remember { mutableStateOf(IntSize.Zero) }
    val onTap: (Offset) -> Unit = { offset ->
        when (tapZone(offset, size, mode)) {
            TapZone.Back -> {
                scope.launch { pager.animateScrollToPage((pager.currentPage - 1).coerceAtLeast(0)) }
            }

            TapZone.Forward -> {
                scope.launch {
                    pager.animateScrollToPage(
                        (pager.currentPage + 1).coerceAtMost(layout.size)
                    )
                }
            }

            TapZone.Middle -> {
                onToggleChrome()
            }
        }
    }
    val page: @Composable (Int) -> Unit = { index ->
        val spread = layout.getOrNull(index)
        if (spread == null) {
            endContent()
        } else {
            key(generation) {
                SpreadContent(spread, urls, mode, failedPages, onTap, onRetryPage)
            }
        }
    }
    Box(modifier.fillMaxSize().onSizeChanged { size = it }) {
        Pager(pager, mode, page)
    }
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
    failedPages: Set<Int>,
    onTap: (Offset) -> Unit,
    onRetryPage: (Int) -> Unit,
) {
    val failed = spread.firstOrNull { it in failedPages }
    when {
        failed != null -> {
            PageFailed(page = failed, onRetry = { onRetryPage(failed) }, modifier = Modifier.fillMaxSize())
        }

        spread.size == 1 -> {
            val state = rememberZoomableImageState()
            Box(Modifier.fillMaxSize()) {
                ZoomableAsyncImage(
                    model = urls[spread.single()],
                    contentDescription = stringResource(R.string.reader_page, spread.single() + 1),
                    state = state,
                    onClick = onTap,
                    modifier = Modifier.fillMaxSize(),
                )
                if (!state.isImageDisplayed) Loading(Modifier.align(Alignment.Center))
            }
        }

        else -> {
            // Facing pages zoom together; in right-to-left order the first page sits on the right.
            val ordered = if (mode == ReaderMode.RightToLeft) spread.reversed() else spread
            Row(Modifier.fillMaxSize().zoomable(rememberZoomableState(), onClick = onTap)) {
                ordered.forEachIndexed { position, page ->
                    AsyncImage(
                        model = urls[page],
                        contentDescription = stringResource(R.string.reader_page, page + 1),
                        // The two halves meet at the gutter.
                        alignment = if (position == 0) Alignment.CenterEnd else Alignment.CenterStart,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                }
            }
        }
    }
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

/** Spreads only make sense in a landscape window wide enough for two readable pages. */
@Composable
private fun isWideWindow(): Boolean {
    val size = LocalWindowInfo.current.containerSize
    val widthDp = with(LocalDensity.current) { size.width.toDp() }
    return size.width > size.height && widthDp >= MIN_SPREAD_WIDTH
}

private val MIN_SPREAD_WIDTH = 600.dp

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
