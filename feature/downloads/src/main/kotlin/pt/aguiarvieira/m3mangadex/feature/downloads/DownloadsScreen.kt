package pt.aguiarvieira.m3mangadex.feature.downloads

import android.text.format.Formatter
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pt.aguiarvieira.m3mangadex.core.designsystem.component.DownloadButton
import pt.aguiarvieira.m3mangadex.core.designsystem.component.Loading
import pt.aguiarvieira.m3mangadex.core.designsystem.component.MangaCover
import pt.aguiarvieira.m3mangadex.core.model.Download
import pt.aguiarvieira.m3mangadex.core.model.DownloadState
import pt.aguiarvieira.m3mangadex.core.designsystem.R as DsR

/** Saved chapters, by manga. A finished one opens in the reader (no network needed). */
@Composable
fun DownloadsRoute(
    onBack: () -> Unit,
    onRead: (mangaId: String, chapterId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DownloadsViewModel = hiltViewModel(),
) {
    val groups by viewModel.groups.collectAsStateWithLifecycle()
    DownloadsScreen(groups, onBack, onRead, viewModel::delete, viewModel::retry, viewModel::deleteManga, modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    groups: List<DownloadGroup>?,
    onBack: () -> Unit,
    onRead: (mangaId: String, chapterId: String) -> Unit,
    onDelete: (String) -> Unit,
    onRetry: (String) -> Unit,
    onDeleteManga: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(R.string.downloads_title))
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack
                    ) { Icon(painterResource(DsR.drawable.ic_arrow_back), stringResource(DsR.string.ds_back)) }
                },
            )
        },
    ) { padding ->
        when {
            groups == null -> {
                Loading(Modifier.fillMaxSize().padding(padding))
            }

            groups.isEmpty() -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(R.string.downloads_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(32.dp),
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding =
                        PaddingValues(
                            top = padding.calculateTopPadding(),
                            bottom =
                                padding.calculateBottomPadding() + 16.dp
                        ),
                ) {
                    item(key = "total") {
                        Text(
                            stringResource(
                                R.string.downloads_total,
                                Formatter.formatShortFileSize(context, groups.sumOf { it.sizeBytes })
                            ),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                    groups.forEach { group ->
                        stickyHeader(key = "manga-${group.mangaId}") { MangaHeader(group, onDeleteManga) }
                        items(group.downloads, key = { it.chapterId }) { download ->
                            DownloadRow(download, onRead, onDelete, onRetry)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MangaHeader(
    group: DownloadGroup,
    onDeleteManga: (String) -> Unit,
) {
    val context = LocalContext.current
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MangaCover(group.coverUrl, contentDescription = null, modifier = Modifier.width(36.dp))
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(
                    group.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    Formatter.formatShortFileSize(context, group.sizeBytes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = { onDeleteManga(group.mangaId) }) {
                Icon(
                    painterResource(DsR.drawable.ic_delete),
                    stringResource(R.string.downloads_delete_manga, group.title)
                )
            }
        }
    }
}

@Composable
private fun DownloadRow(
    download: Download,
    onRead: (mangaId: String, chapterId: String) -> Unit,
    onDelete: (String) -> Unit,
    onRetry: (String) -> Unit,
) {
    val number =
        download.chapterNumber?.let { stringResource(R.string.downloads_chapter, it) }
            ?: stringResource(R.string.downloads_oneshot)
    val context = LocalContext.current
    val status =
        when (download.state) {
            DownloadState.Queued -> {
                stringResource(R.string.downloads_queued)
            }

            DownloadState.Downloading -> {
                stringResource(
                    R.string.downloads_progress,
                    download.pagesDone,
                    download.pageCount
                )
            }

            DownloadState.Failed -> {
                stringResource(R.string.downloads_failed)
            }

            DownloadState.Done -> {
                Formatter.formatShortFileSize(context, download.sizeBytes)
            }
        }
    ListItem(
        onClick = { if (download.state == DownloadState.Done) onRead(download.mangaId, download.chapterId) },
        supportingContent = {
            Column {
                Text(listOf(download.language.uppercase(), status).joinToString(" · "))
                if (download.state == DownloadState.Downloading) {
                    LinearProgressIndicator(
                        progress = { download.progress },
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                    )
                }
            }
        },
        trailingContent = {
            DownloadButton(
                download = download,
                onDownload = {},
                onDelete = { onDelete(download.chapterId) },
                onRetry = { onRetry(download.chapterId) },
            )
        },
    ) {
        Text(
            listOfNotNull(number, download.chapterTitle).joinToString(" · "),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
