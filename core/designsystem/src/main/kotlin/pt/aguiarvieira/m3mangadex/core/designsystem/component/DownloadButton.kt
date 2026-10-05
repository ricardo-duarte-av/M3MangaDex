package pt.aguiarvieira.m3mangadex.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import pt.aguiarvieira.m3mangadex.core.designsystem.R
import pt.aguiarvieira.m3mangadex.core.model.Download
import pt.aguiarvieira.m3mangadex.core.model.DownloadState

/**
 * A chapter's download control: download, waiting, progress (tap to cancel), saved (tap to delete)
 * or failed (tap to retry).
 */
@Composable
fun DownloadButton(
    download: Download?,
    onDownload: () -> Unit,
    onDelete: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state = download?.state
    val (onClick, label) =
        when (state) {
            null -> onDownload to R.string.ds_download
            DownloadState.Failed -> onRetry to R.string.ds_download_retry
            DownloadState.Done -> onDelete to R.string.ds_download_delete
            DownloadState.Queued, DownloadState.Downloading -> onDelete to R.string.ds_download_cancel
        }
    IconButton(onClick = onClick, modifier = modifier) {
        val description = stringResource(label)
        when (state) {
            null -> {
                Icon(painterResource(R.drawable.ic_download), description)
            }

            DownloadState.Queued -> {
                Icon(painterResource(R.drawable.ic_schedule), description)
            }

            DownloadState.Done -> {
                Icon(
                    painterResource(R.drawable.ic_download_done),
                    description,
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            DownloadState.Failed -> {
                Icon(painterResource(R.drawable.ic_error), description, tint = MaterialTheme.colorScheme.error)
            }

            DownloadState.Downloading -> {
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { download.progress },
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.5.dp
                    )
                    Icon(painterResource(R.drawable.ic_close), description, modifier = Modifier.size(12.dp))
                }
            }
        }
    }
}
