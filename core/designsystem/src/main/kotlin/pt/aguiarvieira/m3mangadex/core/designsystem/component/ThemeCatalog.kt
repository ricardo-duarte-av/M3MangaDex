package pt.aguiarvieira.m3mangadex.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * The theme at a glance: the key colour roles, the type scale and a few expressive components.
 * Rendered by the screenshot tests, so any change to the theme (or a cover-derived scheme) shows
 * up as a diff.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ThemeCatalog(modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Surface(modifier = modifier, color = scheme.surface) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("M3MangaDex", style = MaterialTheme.typography.displaySmallEmphasized)
            Text("Headline", style = MaterialTheme.typography.headlineMedium)
            Text("Title", style = MaterialTheme.typography.titleLarge)
            Text("Body text for a chapter description.", style = MaterialTheme.typography.bodyLarge)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Swatch(scheme.primary, scheme.onPrimary, "primary")
                Swatch(scheme.primaryContainer, scheme.onPrimaryContainer, "primaryC")
                Swatch(scheme.secondary, scheme.onSecondary, "secondary")
                Swatch(scheme.secondaryContainer, scheme.onSecondaryContainer, "secondaryC")
                Swatch(scheme.tertiary, scheme.onTertiary, "tertiary")
                Swatch(scheme.tertiaryContainer, scheme.onTertiaryContainer, "tertiaryC")
                Swatch(scheme.surfaceContainerHighest, scheme.onSurface, "surfaceCH")
                Swatch(scheme.error, scheme.onError, "error")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {}) { Text("Read") }
                FilledTonalButton(onClick = {}) { Text("Follow") }
                OutlinedButton(onClick = {}) { Text("Share") }
            }
            LoadingIndicator()
        }
    }
}

@Composable
private fun Swatch(
    container: Color,
    content: Color,
    label: String,
) {
    Box(
        modifier = Modifier.size(width = 88.dp, height = 56.dp).background(container, MaterialTheme.shapes.medium),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = content, style = MaterialTheme.typography.labelSmall)
    }
}
