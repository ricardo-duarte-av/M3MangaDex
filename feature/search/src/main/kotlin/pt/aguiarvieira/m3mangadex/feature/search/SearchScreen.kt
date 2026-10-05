package pt.aguiarvieira.m3mangadex.feature.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import pt.aguiarvieira.m3mangadex.core.designsystem.component.ErrorMessage
import pt.aguiarvieira.m3mangadex.core.designsystem.component.Loading
import pt.aguiarvieira.m3mangadex.core.designsystem.component.MangaCard
import pt.aguiarvieira.m3mangadex.core.designsystem.component.SharedKeys
import pt.aguiarvieira.m3mangadex.core.model.ContentRating
import pt.aguiarvieira.m3mangadex.core.model.Manga
import pt.aguiarvieira.m3mangadex.core.model.MangaFilter
import pt.aguiarvieira.m3mangadex.core.model.MangaOrder
import pt.aguiarvieira.m3mangadex.core.model.coverUrl
import pt.aguiarvieira.m3mangadex.core.designsystem.R as DsR

@Composable
fun SearchRoute(
    args: SearchArgs,
    onBack: () -> Unit,
    onOpenManga: (mangaId: String, coverScope: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel =
        hiltViewModel<SearchViewModel, SearchViewModel.Factory>(key = args.toString()) { it.create(args) },
) {
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val tags by viewModel.tags.collectAsStateWithLifecycle()
    val languages by viewModel.languages.collectAsStateWithLifecycle()
    val onlyMyLanguages by viewModel.onlyMyLanguages.collectAsStateWithLifecycle()
    val results = viewModel.results.collectAsLazyPagingItems()
    val query = rememberTextFieldState(args.query)
    LaunchedEffect(query) { snapshotFlow { query.text.toString() }.collect(viewModel::setTitle) }
    SearchScreen(
        query = query,
        filter = filter,
        selectableRatings = viewModel.selectableRatings,
        results = results,
        languages = languages,
        tags = tags,
        onlyMyLanguages = onlyMyLanguages,
        onFilterChange = viewModel::update,
        onCycleTag = viewModel::cycleTag,
        onOnlyMyLanguagesChange = viewModel::setOnlyMyLanguages,
        onResetFilters = viewModel::resetFilters,
        onRetryTags = viewModel::loadTags,
        onBack = onBack,
        onOpenManga = onOpenManga,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    query: TextFieldState,
    filter: MangaFilter,
    selectableRatings: List<ContentRating>,
    results: LazyPagingItems<Manga>,
    languages: List<String>,
    tags: TagsState,
    onlyMyLanguages: Boolean,
    onFilterChange: ((MangaFilter) -> MangaFilter) -> Unit,
    onCycleTag: (String) -> Unit,
    onOnlyMyLanguagesChange: (Boolean) -> Unit,
    onResetFilters: () -> Unit,
    onRetryTags: () -> Unit,
    onBack: () -> Unit,
    onOpenManga: (mangaId: String, coverScope: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showFilters by rememberSaveable { mutableStateOf(false) }
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(DsR.drawable.ic_arrow_back), stringResource(DsR.string.ds_back))
                    }
                },
                title = { QueryField(query) },
                actions = {
                    SortMenu(
                        order = filter.order,
                        hasTitle = query.text.isNotBlank(),
                        onOrderChange = { order -> onFilterChange { it.copy(order = order) } },
                    )
                    IconButton(onClick = { showFilters = true }) {
                        val count = filter.activeCount + if (onlyMyLanguages) 0 else 1
                        BadgedBox(badge = { if (count > 0) Badge { Text(count.toString()) } }) {
                            Icon(painterResource(DsR.drawable.ic_tune), stringResource(R.string.search_filters))
                        }
                    }
                },
            )
        },
    ) { padding ->
        Results(
            results = results,
            languages = languages,
            onOpenManga = onOpenManga,
            contentPadding =
                PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = padding.calculateTopPadding() + 8.dp,
                    bottom = padding.calculateBottomPadding() + 16.dp,
                ),
        )
    }
    if (showFilters) {
        FilterSheet(
            filter = filter,
            selectableRatings = selectableRatings,
            tags = tags,
            onlyMyLanguages = onlyMyLanguages,
            onFilterChange = onFilterChange,
            onCycleTag = onCycleTag,
            onOnlyMyLanguagesChange = onOnlyMyLanguagesChange,
            onReset = onResetFilters,
            onRetryTags = onRetryTags,
            onDismiss = { showFilters = false },
        )
    }
}

@Composable
private fun QueryField(state: TextFieldState) {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 16.dp)) {
            BasicTextField(
                state = state,
                lineLimits = TextFieldLineLimits.SingleLine,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.weight(1f).padding(vertical = 12.dp),
                decorator = { field ->
                    Box {
                        if (state.text.isEmpty()) {
                            Text(
                                stringResource(R.string.search_hint),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        field()
                    }
                },
            )
            if (state.text.isNotEmpty()) {
                IconButton(onClick = { state.clearText() }) {
                    Icon(painterResource(DsR.drawable.ic_close), stringResource(R.string.search_clear))
                }
            } else {
                Box(Modifier.padding(end = 16.dp))
            }
        }
    }
}

@Composable
private fun SortMenu(
    order: MangaOrder,
    hasTitle: Boolean,
    onOrderChange: (MangaOrder) -> Unit,
) {
    var open by rememberSaveable { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(painterResource(DsR.drawable.ic_sort), stringResource(R.string.search_sort))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            MangaOrder.entries
                .filter { it != MangaOrder.Relevance || hasTitle }
                .forEach { option ->
                    DropdownMenuItem(
                        text = { Text(stringResource(option.label)) },
                        leadingIcon = {
                            if (option == order) Icon(painterResource(DsR.drawable.ic_check), contentDescription = null)
                        },
                        onClick = {
                            open = false
                            onOrderChange(option)
                        },
                    )
                }
        }
    }
}

@Composable
private fun Results(
    results: LazyPagingItems<Manga>,
    languages: List<String>,
    onOpenManga: (mangaId: String, coverScope: String) -> Unit,
    contentPadding: PaddingValues,
) {
    val refresh = results.loadState.refresh
    when {
        results.itemCount == 0 && refresh is LoadState.Loading -> {
            Loading(Modifier.fillMaxSize())
        }

        results.itemCount == 0 && refresh is LoadState.Error -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { ErrorMessage(onRetry = results::retry) }
        }

        results.itemCount == 0 -> {
            Box(Modifier.fillMaxSize().padding(contentPadding), contentAlignment = Alignment.Center) {
                Text(
                    stringResource(R.string.search_no_results),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        else -> {
            ResultGrid(results, languages, onOpenManga, contentPadding)
        }
    }
}

@Composable
private fun ResultGrid(
    results: LazyPagingItems<Manga>,
    languages: List<String>,
    onOpenManga: (mangaId: String, coverScope: String) -> Unit,
    contentPadding: PaddingValues,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 112.dp),
        contentPadding = contentPadding,
        horizontalArrangement =
            androidx.compose.foundation.layout.Arrangement
                .spacedBy(12.dp),
        verticalArrangement =
            androidx.compose.foundation.layout.Arrangement
                .spacedBy(16.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        // No item keys: MangaDex's offset paging can repeat a title across pages as rankings
        // shift, and duplicate keys would crash the grid.
        items(count = results.itemCount) { index ->
            results[index]?.let { manga ->
                MangaCard(
                    title = manga.displayTitle(languages),
                    coverUrl = manga.coverUrl(),
                    onClick = { onOpenManga(manga.id, SEARCH_SCOPE) },
                    sharedKey = SharedKeys.cover(manga.id, SEARCH_SCOPE),
                )
            }
        }
        when (val append = results.loadState.append) {
            is LoadState.Loading -> {
                item(span = { GridItemSpan(maxLineSpan) }) { Loading(Modifier.fillMaxWidth().padding(16.dp)) }
            }

            is LoadState.Error -> {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        ErrorMessage(onRetry = results::retry, message = append.error.message)
                    }
                }
            }

            is LoadState.NotLoading -> {}
        }
    }
}

internal val MangaOrder.label: Int
    get() =
        when (this) {
            MangaOrder.Relevance -> R.string.search_order_relevance
            MangaOrder.Follows -> R.string.search_order_follows
            MangaOrder.Rating -> R.string.search_order_rating
            MangaOrder.LatestUpload -> R.string.search_order_latest
            MangaOrder.Created -> R.string.search_order_created
            MangaOrder.Year -> R.string.search_order_year
            MangaOrder.Title -> R.string.search_order_title
        }

private const val SEARCH_SCOPE = "search"
