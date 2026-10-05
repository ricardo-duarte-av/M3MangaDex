package pt.aguiarvieira.m3mangadex.core.designsystem.component

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier

/** Provided once around the navigation host. */
val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }

/** Provided per destination (the navigation's animated content scope). */
val LocalAnimatedVisibilityScope = compositionLocalOf<AnimatedVisibilityScope?> { null }

/**
 * Keys shared between screens so the same element morphs across a navigation.
 *
 * [scope] names the list a cover was tapped in (`browse:LatestUpdates`, `search`, …) and travels
 * with the navigation to the details header: one manga can sit in several Browse rows at once, and
 * only the tapped copy should fly.
 */
object SharedKeys {
    fun cover(
        mangaId: String,
        scope: String,
    ) = "cover:$scope:$mangaId"
}

/**
 * Marks this element as the same one on the next screen: it grows (or shrinks) between their bounds
 * and crossfades, M3's container transform, during navigation and predictive back. Outside a
 * navigation transition (previews, screenshots) it does nothing.
 */
@Composable
fun Modifier.sharedElement(key: String?): Modifier {
    if (key == null) return this
    val shared = LocalSharedTransitionScope.current ?: return this
    val animated = LocalAnimatedVisibilityScope.current ?: return this
    return with(shared) {
        this@sharedElement.sharedBounds(
            sharedContentState = rememberSharedContentState(key),
            animatedVisibilityScope = animated,
        )
    }
}
