package pt.aguiarvieira.m3mangadex.core.designsystem.component

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import pt.aguiarvieira.m3mangadex.core.designsystem.R
import pt.aguiarvieira.m3mangadex.core.model.ReadingStatus

/** For tabs that only make sense with a MangaDex account: why, and the way in. */
@Composable
fun LoginPrompt(
    title: String,
    body: String,
    onLogin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(24.dp).widthIn(max = 480.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Text(
            body,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Button(onClick = onLogin) { Text(stringResource(R.string.ds_log_in)) }
    }
}

/** The status's name as MangaDex shows it. */
@get:StringRes
val ReadingStatus.label: Int
    get() =
        when (this) {
            ReadingStatus.Reading -> R.string.ds_status_reading
            ReadingStatus.PlanToRead -> R.string.ds_status_plan_to_read
            ReadingStatus.OnHold -> R.string.ds_status_on_hold
            ReadingStatus.ReReading -> R.string.ds_status_re_reading
            ReadingStatus.Completed -> R.string.ds_status_completed
            ReadingStatus.Dropped -> R.string.ds_status_dropped
        }
