package pt.aguiarvieira.m3mangadex.core.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Which cover the whole app should be wearing right now. Screens about one manga [PublishCover] its
 * cover; the app shell themes everything from [current] (navigation bar included), so a screen
 * never shows two palettes. The newest publisher wins; when it leaves, the previous one (or the
 * app's own theme) comes back.
 */
class CoverHolder {
    private val entries = mutableStateListOf<Pair<Any, String?>>()

    val current: String? get() = entries.lastOrNull()?.second

    internal fun add(token: Any) {
        entries += token to null
    }

    internal fun update(
        token: Any,
        url: String?,
    ) {
        val index = entries.indexOfFirst { it.first === token }
        if (index >= 0 && entries[index].second != url) entries[index] = token to url
    }

    internal fun remove(token: Any) {
        entries.removeAll { it.first === token }
    }
}

/** Provided by the app shell; null in previews and tests, where publishing does nothing. */
val LocalCoverHolder = staticCompositionLocalOf<CoverHolder?> { null }

/** Asks the app to wear [coverUrl]'s colours while this composable is on screen. */
@Composable
fun PublishCover(coverUrl: String?) {
    val holder = LocalCoverHolder.current ?: return
    val token = remember { Any() }
    DisposableEffect(holder, token) {
        holder.add(token)
        onDispose { holder.remove(token) }
    }
    SideEffect { holder.update(token, coverUrl) }
}
