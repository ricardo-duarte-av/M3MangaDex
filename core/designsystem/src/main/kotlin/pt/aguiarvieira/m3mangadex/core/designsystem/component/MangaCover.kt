package pt.aguiarvieira.m3mangadex.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage

/** Manga covers are 2:3 portraits almost without exception. */
const val COVER_ASPECT_RATIO = 2f / 3f

/** A cover image at the 2:3 cover ratio, on a tonal placeholder while it loads. */
@Composable
fun MangaCover(
    url: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.medium,
) {
    AsyncImage(
        model = url,
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        modifier =
            modifier
                .aspectRatio(COVER_ASPECT_RATIO)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
    )
}
