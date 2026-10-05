package pt.aguiarvieira.m3mangadex.core.designsystem.theme

import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicColorScheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Whether screens may retint themselves from cover art (the user's setting, provided by the app). */
val LocalCoverTheming = staticCompositionLocalOf { true }

/**
 * Wraps [content] in a colour scheme seeded from [coverUrl]'s cover art. The colours glide from
 * the surrounding theme to the cover's (and back, when the cover changes or theming is off) instead
 * of snapping, so opening a manga feels like it brings its colours with it.
 *
 * TonalSpot, Android's own wallpaper style, keeps accents close to the cover's hue; the livelier
 * styles rotate hues away from the art.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CoverTheme(
    coverUrl: String?,
    content: @Composable () -> Unit,
) {
    val ambient = MaterialTheme.colorScheme
    val seed = if (LocalCoverTheming.current) rememberCoverSeed(coverUrl) else null
    val seeded =
        seed?.let {
            rememberDynamicColorScheme(
                seedColor = it,
                isDark = isSystemInDarkTheme(),
                style = PaletteStyle.TonalSpot,
                specVersion = ColorSpec.SpecVersion.SPEC_2025,
            )
        }
    MaterialExpressiveTheme(
        colorScheme = animateColorScheme(seeded ?: ambient, MaterialTheme.motionScheme.slowEffectsSpec()),
        motionScheme = MaterialTheme.motionScheme,
        typography = MaterialTheme.typography,
        shapes = MaterialTheme.shapes,
        content = content,
    )
}

@Composable
private fun rememberCoverSeed(url: String?): Color? {
    val context = LocalContext.current
    var seed by remember(url) { mutableStateOf(url?.let(CoverSeed::cached)) }
    LaunchedEffect(url) {
        if (url != null && !CoverSeed.isCached(url)) seed = extractSeed(context, url)
    }
    return seed
}

/** Loads the cover as a software bitmap (seeding reads pixels) and picks its seed. */
private suspend fun extractSeed(
    context: Context,
    url: String,
): Color? =
    withContext(Dispatchers.Default) {
        val request =
            ImageRequest
                .Builder(context)
                .data(url)
                .allowHardware(false)
                .build()
        val bitmap =
            (context.imageLoader.execute(request) as? SuccessResult)?.image?.toBitmap() ?: return@withContext null
        CoverSeed.from(url, bitmap)
    }

/** Every colour role of [target], each animated towards its new value with [spec]. */
@Composable
fun animateColorScheme(
    target: ColorScheme,
    spec: AnimationSpec<Color>,
): ColorScheme {
    @Composable
    fun animate(color: Color): Color = animateColorAsState(color, spec, label = "scheme").value
    return target.copy(
        primary = animate(target.primary),
        onPrimary = animate(target.onPrimary),
        primaryContainer = animate(target.primaryContainer),
        onPrimaryContainer = animate(target.onPrimaryContainer),
        inversePrimary = animate(target.inversePrimary),
        secondary = animate(target.secondary),
        onSecondary = animate(target.onSecondary),
        secondaryContainer = animate(target.secondaryContainer),
        onSecondaryContainer = animate(target.onSecondaryContainer),
        tertiary = animate(target.tertiary),
        onTertiary = animate(target.onTertiary),
        tertiaryContainer = animate(target.tertiaryContainer),
        onTertiaryContainer = animate(target.onTertiaryContainer),
        background = animate(target.background),
        onBackground = animate(target.onBackground),
        surface = animate(target.surface),
        onSurface = animate(target.onSurface),
        surfaceVariant = animate(target.surfaceVariant),
        onSurfaceVariant = animate(target.onSurfaceVariant),
        surfaceTint = animate(target.surfaceTint),
        inverseSurface = animate(target.inverseSurface),
        inverseOnSurface = animate(target.inverseOnSurface),
        error = animate(target.error),
        onError = animate(target.onError),
        errorContainer = animate(target.errorContainer),
        onErrorContainer = animate(target.onErrorContainer),
        outline = animate(target.outline),
        outlineVariant = animate(target.outlineVariant),
        scrim = animate(target.scrim),
        surfaceBright = animate(target.surfaceBright),
        surfaceDim = animate(target.surfaceDim),
        surfaceContainer = animate(target.surfaceContainer),
        surfaceContainerHigh = animate(target.surfaceContainerHigh),
        surfaceContainerHighest = animate(target.surfaceContainerHighest),
        surfaceContainerLow = animate(target.surfaceContainerLow),
        surfaceContainerLowest = animate(target.surfaceContainerLowest),
        primaryFixed = animate(target.primaryFixed),
        primaryFixedDim = animate(target.primaryFixedDim),
        onPrimaryFixed = animate(target.onPrimaryFixed),
        onPrimaryFixedVariant = animate(target.onPrimaryFixedVariant),
        secondaryFixed = animate(target.secondaryFixed),
        secondaryFixedDim = animate(target.secondaryFixedDim),
        onSecondaryFixed = animate(target.onSecondaryFixed),
        onSecondaryFixedVariant = animate(target.onSecondaryFixedVariant),
        tertiaryFixed = animate(target.tertiaryFixed),
        tertiaryFixedDim = animate(target.tertiaryFixedDim),
        onTertiaryFixed = animate(target.onTertiaryFixed),
        onTertiaryFixedVariant = animate(target.onTertiaryFixedVariant),
    )
}
