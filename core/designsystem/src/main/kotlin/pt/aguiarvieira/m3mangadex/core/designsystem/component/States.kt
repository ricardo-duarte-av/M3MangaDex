package pt.aguiarvieira.m3mangadex.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import pt.aguiarvieira.m3mangadex.core.designsystem.R

/** The expressive loading indicator, centred in whatever space it is given. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun Loading(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) { LoadingIndicator() }
}

/** A failure with a way out. [message] null shows the generic connection hint. */
@Composable
fun ErrorMessage(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    message: String? = null,
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = message ?: stringResource(R.string.ds_error_generic),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        FilledTonalButton(onClick = onRetry) { Text(stringResource(R.string.ds_retry)) }
    }
}

/** A row's heading, with an optional "See all". */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    onSeeAll: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.weight(1f).padding(vertical = 12.dp),
        )
        if (onSeeAll != null) {
            TextButton(onClick = onSeeAll) { Text(stringResource(R.string.ds_see_all)) }
        }
    }
}
