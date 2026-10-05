package pt.aguiarvieira.m3mangadex.feature.reader

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import pt.aguiarvieira.m3mangadex.core.designsystem.component.Loading
import pt.aguiarvieira.m3mangadex.core.model.ReaderMode

/**
 * A continuous strip for webtoons: pages edge to edge, as wide as the screen (capped on tablets so
 * panels stay a readable size). Taps on the top or bottom quarter scroll most of a screen; the
 * middle toggles the chrome. [endContent] closes the strip.
 */
@Composable
internal fun WebtoonReader(
    urls: List<String>,
    generation: Int,
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
    val list = rememberLazyListState(initialFirstVisibleItemIndex = startPage)
    val scope = rememberCoroutineScope()
    var size by remember { mutableStateOf(IntSize.Zero) }
    var current by remember { mutableIntStateOf(startPage) }
    val pageShown by rememberUpdatedState(onPageShow)
    val endShown by rememberUpdatedState(onEndShow)

    LaunchedEffect(list) {
        snapshotFlow {
            val lastVisible =
                list.layoutInfo.visibleItemsInfo
                    .lastOrNull()
                    ?.index
            if (lastVisible == urls.size) urls.size else list.firstVisibleItemIndex
        }.distinctUntilChanged().collect { index ->
            if (index >= urls.size) {
                endShown()
            } else {
                current = index
                pageShown(index)
            }
        }
    }
    LaunchedEffect(list, commands) {
        commands.collect { command ->
            when (command) {
                is ReaderCommand.Jump -> list.scrollToItem(command.page.coerceIn(0, urls.size))
                is ReaderCommand.Step -> list.animateScrollBy(command.delta * size.height * SCREEN_STEP)
            }
        }
    }
    PreloadPages(urls, generation, current, onSize = { _, _, _ -> }, onFail = onPageFail)

    LazyColumn(
        state = list,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier =
            modifier
                .fillMaxSize()
                .onSizeChanged { size = it }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        when (tapZone(offset, size, ReaderMode.Webtoon)) {
                            TapZone.Back -> scope.launch { list.animateScrollBy(-size.height * SCREEN_STEP) }
                            TapZone.Forward -> scope.launch { list.animateScrollBy(size.height * SCREEN_STEP) }
                            TapZone.Middle -> onToggleChrome()
                        }
                    }
                },
    ) {
        items(count = urls.size, key = { it }) { page ->
            key(generation) {
                StripPage(
                    url = urls[page],
                    page = page,
                    failed = page in failedPages,
                    onFail = { onPageFail(page) },
                    onRetry = { onRetryPage(page) },
                    modifier = Modifier.widthIn(max = MAX_STRIP_WIDTH).fillMaxWidth(),
                )
            }
        }
        // Clear of the navigation bar and the reader's bottom bar, so its buttons stay tappable.
        item(
            key = "end"
        ) { Box(Modifier.navigationBarsPadding().padding(bottom = END_BOTTOM_CLEARANCE)) { endContent() } }
    }
}

@Composable
private fun StripPage(
    url: String,
    page: Int,
    failed: Boolean,
    onFail: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (failed) {
        PageFailed(page, onRetry, modifier.height(PLACEHOLDER_HEIGHT))
        return
    }
    val painter = rememberAsyncImagePainter(model = url, onError = { onFail() })
    val state by painter.state.collectAsStateWithLifecycle()
    when (state) {
        is AsyncImagePainter.State.Success -> {
            val intrinsic = painter.intrinsicSize
            Image(
                painter = painter,
                contentDescription = stringResource(R.string.reader_page, page + 1),
                contentScale = ContentScale.FillWidth,
                modifier =
                    modifier.aspectRatio((intrinsic.width / intrinsic.height).takeIf { it.isFinite() && it > 0 } ?: 1f),
            )
        }

        // A tall placeholder, so the strip below doesn't jump far when the page arrives.
        else -> {
            Box(modifier.height(PLACEHOLDER_HEIGHT), contentAlignment = Alignment.Center) { Loading() }
        }
    }
}

private const val SCREEN_STEP = 0.8f
private val MAX_STRIP_WIDTH = 720.dp
private val PLACEHOLDER_HEIGHT = 600.dp
private val END_BOTTOM_CLEARANCE = 120.dp
