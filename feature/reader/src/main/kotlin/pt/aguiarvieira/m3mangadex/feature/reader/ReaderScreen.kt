package pt.aguiarvieira.m3mangadex.feature.reader

import android.content.ActivityNotFoundException
import android.content.Context
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import pt.aguiarvieira.m3mangadex.core.designsystem.component.ErrorMessage
import pt.aguiarvieira.m3mangadex.core.designsystem.component.Loading
import pt.aguiarvieira.m3mangadex.core.designsystem.theme.PublishCover
import pt.aguiarvieira.m3mangadex.core.model.Chapter
import pt.aguiarvieira.m3mangadex.core.model.PageFit
import pt.aguiarvieira.m3mangadex.core.model.ReaderMode
import pt.aguiarvieira.m3mangadex.core.model.coverUrl
import kotlin.math.roundToInt
import pt.aguiarvieira.m3mangadex.core.designsystem.R as DsR

/**
 * The reader. [onOpenChapter] replaces this chapter with another one hosted on MangaDex; chapters
 * on a publisher's site open in a Custom Tab instead.
 */
@Composable
fun ReaderRoute(
    mangaId: String,
    chapterId: String,
    onBack: () -> Unit,
    onOpenChapter: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReaderViewModel =
        hiltViewModel<ReaderViewModel, ReaderViewModel.Factory>(
            key = "reader:$chapterId"
        ) { it.create(mangaId, chapterId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    // The reader keeps wearing the manga's cover colours, like its details screen.
    PublishCover(state.manga?.coverUrl())
    ReaderScreen(
        state = state,
        onBack = onBack,
        onOpenChapter = { chapter ->
            if (chapter.isExternal) openExternal(context, chapter.externalUrl!!) else onOpenChapter(chapter.id)
        },
        onPageShow = viewModel::onPageShow,
        onPageFail = viewModel::onPageFail,
        onRetryPage = viewModel::retryPage,
        onRetry = viewModel::load,
        onModeChange = viewModel::setMode,
        onSpreadsChange = viewModel::setDoublePageSpreads,
        onVolumeKeysChange = viewModel::setVolumeKeyPaging,
        onPageFitChange = viewModel::setPageFit,
        onCropBordersChange = viewModel::setCropBorders,
        modifier = modifier,
    )
}

private fun openExternal(
    context: Context,
    url: String,
) {
    try {
        CustomTabsIntent
            .Builder()
            .setShowTitle(true)
            .build()
            .launchUrl(context, url.toUri())
    } catch (_: ActivityNotFoundException) {
        // No browser at all; nothing sensible to do.
    }
}

@Composable
fun ReaderScreen(
    state: ReaderUiState,
    onBack: () -> Unit,
    onOpenChapter: (Chapter) -> Unit,
    onPageShow: (Int) -> Unit,
    onPageFail: (Int) -> Unit,
    onRetryPage: (Int) -> Unit,
    onRetry: () -> Unit,
    onModeChange: (ReaderMode) -> Unit,
    onSpreadsChange: (Boolean) -> Unit,
    onVolumeKeysChange: (Boolean) -> Unit,
    onPageFitChange: (PageFit) -> Unit,
    onCropBordersChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var chrome by rememberSaveable { mutableStateOf(true) }
    var page by rememberSaveable(state.chapter?.id) { mutableIntStateOf(state.startPage) }
    val commands = remember { MutableSharedFlow<ReaderCommand>(extraBufferCapacity = 8) }
    val focus = remember { FocusRequester() }
    ImmersiveWhile(hidden = !chrome)
    KeepScreenOn()

    val pages = state.pages
    // Out of the way once the chapter is on screen; a tap in the middle brings it back.
    LaunchedEffect(pages is PagesState.Loaded) {
        if (pages is PagesState.Loaded) {
            delay(CHROME_AUTO_HIDE_MILLIS)
            chrome = false
        }
    }
    LaunchedEffect(Unit) { focus.requestFocus() }

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(Color.Black)
                .focusRequester(focus)
                .focusable()
                .onPreviewKeyEvent { event ->
                    val delta =
                        when (event.key) {
                            Key.VolumeDown -> 1
                            Key.VolumeUp -> -1
                            else -> 0
                        }
                    if (!state.volumeKeyPaging || delta == 0) return@onPreviewKeyEvent false
                    if (event.type == KeyEventType.KeyDown) commands.tryEmit(ReaderCommand.Step(delta))
                    true
                },
    ) {
        val chapter = state.chapter
        when {
            pages is PagesState.Failed -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    ErrorMessage(onRetry = onRetry, message = stringResource(R.string.reader_failed))
                }
            }

            pages is PagesState.Loaded && chapter != null -> {
                val end: @Composable () -> Unit = {
                    EndOfChapter(chapter, state.neighbors, onOpenChapter, onBack)
                }
                val onShown: (Int) -> Unit = {
                    page = it
                    onPageShow(it)
                }
                val toggle = { chrome = !chrome }
                if (state.mode == ReaderMode.Webtoon) {
                    WebtoonReader(
                        urls = pages.urls,
                        generation = pages.generation,
                        startPage = page,
                        failedPages = state.failedPages,
                        commands = commands,
                        onPageShow = onShown,
                        onEndShow = { onPageShow(pages.urls.lastIndex) },
                        onToggleChrome = toggle,
                        onPageFail = onPageFail,
                        onRetryPage = onRetryPage,
                        endContent = end,
                    )
                } else {
                    PagedReader(
                        urls = pages.urls,
                        generation = pages.generation,
                        mode = state.mode,
                        spreadsEnabled = state.doublePageSpreads,
                        fit = state.pageFit,
                        cropBorders = state.cropBorders,
                        startPage = page,
                        failedPages = state.failedPages,
                        commands = commands,
                        onPageShow = onShown,
                        onEndShow = { onPageShow(pages.urls.lastIndex) },
                        onToggleChrome = toggle,
                        onPageFail = onPageFail,
                        onRetryPage = onRetryPage,
                        endContent = end,
                    )
                }
            }

            else -> {
                Loading(Modifier.fillMaxSize())
            }
        }

        ReaderChrome(
            visible = chrome || pages !is PagesState.Loaded,
            state = state,
            page = page,
            pageCount = (pages as? PagesState.Loaded)?.urls?.size ?: 0,
            onBack = onBack,
            onOpenChapter = onOpenChapter,
            onJump = { commands.tryEmit(ReaderCommand.Jump(it)) },
            onModeChange = onModeChange,
            onSpreadsChange = onSpreadsChange,
            onVolumeKeysChange = onVolumeKeysChange,
            onPageFitChange = onPageFitChange,
            onCropBordersChange = onCropBordersChange,
        )
    }
}

@Composable
private fun ReaderChrome(
    visible: Boolean,
    state: ReaderUiState,
    page: Int,
    pageCount: Int,
    onBack: () -> Unit,
    onOpenChapter: (Chapter) -> Unit,
    onJump: (Int) -> Unit,
    onModeChange: (ReaderMode) -> Unit,
    onSpreadsChange: (Boolean) -> Unit,
    onVolumeKeysChange: (Boolean) -> Unit,
    onPageFitChange: (PageFit) -> Unit,
    onCropBordersChange: (Boolean) -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = CHROME_ALPHA),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(Modifier.statusBarsPadding().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(DsR.drawable.ic_arrow_back), stringResource(DsR.string.ds_back))
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            state.manga?.displayTitle(state.languages).orEmpty(),
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        state.chapter?.let {
                            Text(
                                it.label(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    OptionsMenu(
                        state,
                        onModeChange,
                        onSpreadsChange,
                        onVolumeKeysChange,
                        onPageFitChange,
                        onCropBordersChange
                    )
                }
            }
        }
        AnimatedVisibility(
            visible = visible && pageCount > 0,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = CHROME_ALPHA),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier.navigationBarsPadding().padding(horizontal = 4.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val previous = state.neighbors.previous
                    val next = state.neighbors.next
                    // In right-to-left reading the previous chapter is on the right, as in a book.
                    val (start, end) = if (state.mode == ReaderMode.RightToLeft) next to previous else previous to next
                    ChapterButton(start, isNext = start == next, icon = DsR.drawable.ic_chevron_left, onOpenChapter)
                    PageSlider(page, pageCount, rtl = state.mode == ReaderMode.RightToLeft, onJump, Modifier.weight(1f))
                    ChapterButton(end, isNext = end == next, icon = DsR.drawable.ic_chevron_right, onOpenChapter)
                }
            }
        }
    }
}

@Composable
private fun ChapterButton(
    chapter: Chapter?,
    isNext: Boolean,
    icon: Int,
    onOpenChapter: (Chapter) -> Unit,
) {
    IconButton(onClick = { chapter?.let(onOpenChapter) }, enabled = chapter != null) {
        Icon(
            painterResource(icon),
            stringResource(if (isNext) R.string.reader_next_chapter else R.string.reader_previous_chapter),
        )
    }
}

/** The page scrubber; drags preview the number, releasing jumps. Mirrored for right-to-left reading. */
@Composable
private fun PageSlider(
    page: Int,
    pageCount: Int,
    rtl: Boolean,
    onJump: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var dragging by remember { mutableStateOf(false) }
    val slider =
        remember(pageCount) {
            SliderState(
                value = page.toFloat(),
                trackRange =
                    0f..(pageCount - 1).coerceAtLeast(1).toFloat()
            )
        }
    LaunchedEffect(page) { if (!dragging) slider.value = page.toFloat() }
    Column(modifier.padding(horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            stringResource(R.string.reader_page_of, slider.value.roundToInt() + 1, pageCount),
            style = MaterialTheme.typography.labelMedium,
        )
        CompositionLocalProvider(LocalLayoutDirection provides if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr) {
            Slider(
                state = slider,
                onValueChange = {
                    dragging = true
                    slider.value = it
                },
                onValueChangeFinished = {
                    dragging = false
                    onJump(slider.value.roundToInt())
                },
            )
        }
    }
}

@Composable
private fun OptionsMenu(
    state: ReaderUiState,
    onModeChange: (ReaderMode) -> Unit,
    onSpreadsChange: (Boolean) -> Unit,
    onVolumeKeysChange: (Boolean) -> Unit,
    onPageFitChange: (PageFit) -> Unit,
    onCropBordersChange: (Boolean) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(painterResource(DsR.drawable.ic_tune), stringResource(R.string.reader_options))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            ReaderMode.entries.forEach { mode ->
                DropdownMenuItem(
                    text = { Text(stringResource(mode.label)) },
                    leadingIcon = {
                        if (mode ==
                            state.mode
                        ) {
                            Icon(painterResource(DsR.drawable.ic_check), contentDescription = null)
                        }
                    },
                    onClick = {
                        open = false
                        onModeChange(mode)
                    },
                )
            }
            if (state.mode.isPaged) {
                HorizontalDivider()
                PageFit.entries.forEach { fit ->
                    DropdownMenuItem(
                        text = { Text(stringResource(fit.label)) },
                        leadingIcon = {
                            if (fit ==
                                state.pageFit
                            ) {
                                Icon(painterResource(DsR.drawable.ic_check), contentDescription = null)
                            }
                        },
                        onClick = { onPageFitChange(fit) },
                    )
                }
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.reader_crop_borders)) },
                    trailingIcon = { Switch(checked = state.cropBorders, onCheckedChange = null) },
                    onClick = { onCropBordersChange(!state.cropBorders) },
                )
            }
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text(stringResource(R.string.reader_spreads)) },
                trailingIcon = { Switch(checked = state.doublePageSpreads, onCheckedChange = null) },
                onClick = { onSpreadsChange(!state.doublePageSpreads) },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.reader_volume_keys)) },
                trailingIcon = { Switch(checked = state.volumeKeyPaging, onCheckedChange = null) },
                onClick = { onVolumeKeysChange(!state.volumeKeyPaging) },
            )
        }
    }
}

private val ReaderMode.label: Int
    get() =
        when (this) {
            ReaderMode.LeftToRight -> R.string.reader_mode_ltr
            ReaderMode.RightToLeft -> R.string.reader_mode_rtl
            ReaderMode.Vertical -> R.string.reader_mode_vertical
            ReaderMode.Webtoon -> R.string.reader_mode_webtoon
        }

internal val PageFit.label: Int
    get() =
        when (this) {
            PageFit.Auto -> R.string.reader_fit_auto
            PageFit.Screen -> R.string.reader_fit_screen
            PageFit.Width -> R.string.reader_fit_width
            PageFit.Height -> R.string.reader_fit_height
        }

private const val CHROME_ALPHA = 0.92f
private const val CHROME_AUTO_HIDE_MILLIS = 2_500L
