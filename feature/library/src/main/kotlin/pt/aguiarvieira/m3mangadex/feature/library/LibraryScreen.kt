package pt.aguiarvieira.m3mangadex.feature.library

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pt.aguiarvieira.m3mangadex.core.designsystem.component.ErrorMessage
import pt.aguiarvieira.m3mangadex.core.designsystem.component.Loading
import pt.aguiarvieira.m3mangadex.core.designsystem.component.LoginPrompt
import pt.aguiarvieira.m3mangadex.core.designsystem.component.MangaCard
import pt.aguiarvieira.m3mangadex.core.designsystem.component.label
import pt.aguiarvieira.m3mangadex.core.model.ReadingStatus
import pt.aguiarvieira.m3mangadex.core.model.coverUrl
import pt.aguiarvieira.m3mangadex.core.designsystem.R as DsR

@Composable
fun LibraryRoute(
    onOpenManga: (String) -> Unit,
    onLogin: () -> Unit,
    onOpenDownloads: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val languages by viewModel.languages.collectAsStateWithLifecycle(emptyList())
    LibraryScreen(
        state = state,
        languages = languages,
        onFilter = viewModel::setFilter,
        onRefresh = viewModel::refresh,
        onOpenManga = onOpenManga,
        onLogin = onLogin,
        onOpenDownloads = onOpenDownloads,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LibraryScreen(
    state: LibraryUiState,
    languages: List<String>,
    onFilter: (ReadingStatus?) -> Unit,
    onRefresh: () -> Unit,
    onOpenManga: (String) -> Unit,
    onLogin: () -> Unit,
    onOpenDownloads: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            MediumFlexibleTopAppBar(
                title = { Text(stringResource(R.string.library_title)) },
                actions = {
                    IconButton(onClick = onOpenDownloads) {
                        Icon(painterResource(DsR.drawable.ic_download_done), stringResource(R.string.library_downloads))
                    }
                },
            )
        },
    ) { padding ->
        val content = Modifier.fillMaxSize().padding(top = padding.calculateTopPadding())
        when (state) {
            LibraryUiState.LoggedOut -> {
                Box(content, contentAlignment = Alignment.Center) {
                    LoginPrompt(
                        title = stringResource(R.string.library_logged_out_title),
                        body = stringResource(R.string.library_logged_out_body),
                        onLogin = onLogin,
                    )
                }
            }

            LibraryUiState.Loading -> {
                Loading(content)
            }

            LibraryUiState.Failed -> {
                Box(content, contentAlignment = Alignment.Center) { ErrorMessage(onRetry = onRefresh) }
            }

            is LibraryUiState.Loaded -> {
                PullToRefreshBox(isRefreshing = state.refreshing, onRefresh = onRefresh, modifier = content) {
                    Column {
                        StatusFilters(state, onFilter)
                        if (state.shown.isEmpty()) {
                            Text(
                                stringResource(R.string.library_empty),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(32.dp).align(Alignment.CenterHorizontally),
                            )
                        }
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 112.dp),
                            contentPadding = PaddingValues(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            items(state.shown, key = { it.manga.id }) { entry ->
                                MangaCard(
                                    title = entry.manga.displayTitle(languages),
                                    coverUrl = entry.manga.coverUrl(),
                                    onClick = { onOpenManga(entry.manga.id) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusFilters(
    state: LibraryUiState.Loaded,
    onFilter: (ReadingStatus?) -> Unit,
) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = state.filter == null,
            onClick = { onFilter(null) },
            label = {
                Text(
                    stringResource(
                        R.string.library_status_count,
                        stringResource(R.string.library_all),
                        state.entries.size
                    )
                )
            },
        )
        ReadingStatus.entries.filter { state.count(it) > 0 }.forEach { status ->
            FilterChip(
                selected = state.filter == status,
                onClick = { onFilter(status) },
                label = {
                    Text(
                        stringResource(R.string.library_status_count, stringResource(status.label), state.count(status))
                    )
                },
            )
        }
    }
}
