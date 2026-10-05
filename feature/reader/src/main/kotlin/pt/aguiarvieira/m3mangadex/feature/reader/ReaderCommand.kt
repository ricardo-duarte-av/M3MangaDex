package pt.aguiarvieira.m3mangadex.feature.reader

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import coil3.imageLoader
import coil3.request.ImageRequest

/** What the chrome, tap zones and volume keys ask the active reader layout to do. */
sealed interface ReaderCommand {
    /** Show [page] (zero-based); the layout picks the spread or strip position holding it. */
    data class Jump(
        val page: Int,
    ) : ReaderCommand

    /** Move one screen forward (+1) or back (-1) in reading order. */
    data class Step(
        val delta: Int,
    ) : ReaderCommand
}

/** Hides the status and navigation bars while [hidden]; a swipe from the edge peeks them. */
@Composable
internal fun ImmersiveWhile(hidden: Boolean) {
    val view = LocalView.current
    val window = (view.context as? Activity)?.window ?: return
    DisposableEffect(hidden) {
        val controller = WindowCompat.getInsetsController(window, view)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        if (hidden) {
            controller.hide(
                WindowInsetsCompat.Type.systemBars()
            )
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
        onDispose { controller.show(WindowInsetsCompat.Type.systemBars()) }
    }
}

/** No screen timeout while reading. */
@Composable
internal fun KeepScreenOn() {
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
}

/**
 * Fetches the pages just ahead of [current] into Coil's caches, so turning a page shows it at once.
 * Each finished load reports its size ([onSize]: spreads need to know which pages are wide) or its
 * failure ([onFail]: the page server may need replacing).
 */
@Composable
internal fun PreloadPages(
    urls: List<String>,
    generation: Int,
    current: Int,
    onSize: (page: Int, width: Int, height: Int) -> Unit,
    onFail: (page: Int) -> Unit,
    ahead: Int = PRELOAD_AHEAD,
) {
    val context = LocalContext.current
    val requested = remember(urls, generation) { mutableSetOf<Int>() }
    val sizeCallback by rememberUpdatedState(onSize)
    val failCallback by rememberUpdatedState(onFail)
    LaunchedEffect(urls, generation, current) {
        val loader = context.imageLoader
        for (page in current..(current + ahead).coerceAtMost(urls.lastIndex)) {
            if (!requested.add(page)) continue
            loader.enqueue(
                ImageRequest
                    .Builder(context)
                    .data(urls[page])
                    .listener(
                        onSuccess = { _, result -> sizeCallback(page, result.image.width, result.image.height) },
                        onError = { _, _ -> failCallback(page) },
                    ).build(),
            )
        }
    }
}

private const val PRELOAD_AHEAD = 4
