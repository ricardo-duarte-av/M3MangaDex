package pt.aguiarvieira.m3mangadex.feature.browse

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.AppBarWithSearch
import androidx.compose.material3.ExpandedFullScreenSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarState
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.Text
import androidx.compose.material3.carousel.HorizontalCenteredHeroCarousel
import androidx.compose.material3.carousel.HorizontalUncontainedCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import pt.aguiarvieira.m3mangadex.core.data.BrowseSection
import pt.aguiarvieira.m3mangadex.core.designsystem.component.ErrorMessage
import pt.aguiarvieira.m3mangadex.core.designsystem.component.Loading
import pt.aguiarvieira.m3mangadex.core.designsystem.component.MangaCard
import pt.aguiarvieira.m3mangadex.core.designsystem.component.MangaCover
import pt.aguiarvieira.m3mangadex.core.designsystem.component.SectionHeader
import pt.aguiarvieira.m3mangadex.core.designsystem.component.SharedKeys
import pt.aguiarvieira.m3mangadex.core.designsystem.component.sharedElement
import pt.aguiarvieira.m3mangadex.core.model.Covers
import pt.aguiarvieira.m3mangadex.core.model.Manga
import pt.aguiarvieira.m3mangadex.core.model.coverUrl
import pt.aguiarvieira.m3mangadex.core.designsystem.R as DsR

/**
 * The Browse home: a search bar (live title suggestions; submitting opens full search) over a hero
 * carousel of popular new titles and rows for the other [BrowseSection]s.
 */
@Composable
fun BrowseRoute(
    onOpenManga: (mangaId: String, coverScope: String) -> Unit,
    onOpenSearch: (query: String, section: BrowseSection?) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BrowseViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val suggestions by viewModel.suggestions.collectAsStateWithLifecycle()
    BrowseScreen(
        state = state,
        suggestions = suggestions,
        onQueryChange = viewModel::onQueryChange,
        onRefresh = viewModel::refresh,
        onOpenManga = onOpenManga,
        onOpenSearch = onOpenSearch,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BrowseScreen(
    state: BrowseUiState,
    suggestions: Suggestions,
    onQueryChange: (String) -> Unit,
    onRefresh: () -> Unit,
    onOpenManga: (mangaId: String, coverScope: String) -> Unit,
    onOpenSearch: (query: String, section: BrowseSection?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val searchBarState = rememberSearchBarState()
    val textFieldState = rememberTextFieldState()
    val scope = rememberCoroutineScope()
    LaunchedEffect(textFieldState) { snapshotFlow { textFieldState.text.toString() }.collect(onQueryChange) }

    val collapseThen: (() -> Unit) -> Unit = { action ->
        scope.launch {
            searchBarState.animateToCollapsed()
            action()
        }
    }
    val onSearch: (
        String,
    ) -> Unit = { query -> if (query.isNotBlank()) collapseThen { onOpenSearch(query.trim(), null) } }
    val inputField: @Composable () -> Unit = {
        SearchInputField(textFieldState, searchBarState, onSearch, onCollapse = { collapseThen {} })
    }
    // The collapsed bar never takes focus itself: with a hardware keyboard (or after any start in
    // non-touch mode) Android hands initial focus to the first focusable view, and a focused
    // search field expands — the search would pop open on launch. Tapping it expands instead.
    val hint = stringResource(R.string.browse_search_hint)
    val collapsedField: @Composable () -> Unit = {
        Box {
            SearchInputField(
                textFieldState,
                searchBarState,
                onSearch,
                onCollapse = { collapseThen {} },
                modifier = Modifier.focusProperties { canFocus = false },
            )
            Box(
                Modifier
                    .matchParentSize()
                    .clickable(role = Role.Button) { scope.launch { searchBarState.animateToExpanded() } }
                    .semantics { contentDescription = hint },
            )
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            AppBarWithSearch(
                state = searchBarState,
                inputField = collapsedField,
                actions = {
                    IconButton(onClick = { onOpenSearch("", null) }) {
                        Icon(painterResource(DsR.drawable.ic_tune), stringResource(R.string.browse_advanced_search))
                    }
                },
            )
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.refreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize().padding(top = padding.calculateTopPadding()),
        ) {
            BrowseSections(
                state = state,
                onOpenManga = onOpenManga,
                onSeeAll = { onOpenSearch("", it) },
                onRetry = onRefresh,
                contentPadding = PaddingValues(bottom = padding.calculateBottomPadding() + 16.dp),
            )
        }
    }

    ExpandedFullScreenSearchBar(state = searchBarState, inputField = inputField) {
        SuggestionList(
            suggestions = suggestions,
            languages = state.languages,
            onOpenManga = { id -> collapseThen { onOpenManga(id, SUGGESTIONS_SCOPE) } },
            onSearchAll = { query -> collapseThen { onOpenSearch(query, null) } },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchInputField(
    textFieldState: TextFieldState,
    searchBarState: SearchBarState,
    onSearch: (String) -> Unit,
    onCollapse: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SearchBarDefaults.InputField(
        textFieldState = textFieldState,
        searchBarState = searchBarState,
        onSearch = onSearch,
        modifier = modifier,
        placeholder = { Text(stringResource(R.string.browse_search_hint)) },
        leadingIcon = {
            if (searchBarState.targetValue == SearchBarValue.Expanded) {
                IconButton(onClick = onCollapse) {
                    Icon(painterResource(DsR.drawable.ic_arrow_back), stringResource(R.string.browse_collapse))
                }
            } else {
                Icon(painterResource(DsR.drawable.ic_search), contentDescription = null)
            }
        },
        trailingIcon = {
            if (textFieldState.text.isNotEmpty()) {
                IconButton(onClick = { textFieldState.clearText() }) {
                    Icon(painterResource(DsR.drawable.ic_close), stringResource(R.string.browse_clear))
                }
            }
        },
    )
}

@Composable
private fun BrowseSections(
    state: BrowseUiState,
    onOpenManga: (mangaId: String, coverScope: String) -> Unit,
    onSeeAll: (BrowseSection) -> Unit,
    onRetry: () -> Unit,
    contentPadding: PaddingValues,
) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = contentPadding) {
        BrowseSection.entries.forEach { section ->
            item(key = section.name) {
                SectionHeader(title = stringResource(section.title), onSeeAll = { onSeeAll(section) })
                val height = if (section == BrowseSection.PopularNew) HeroHeight else RowHeight
                when (val sectionState = state.sections[section] ?: SectionState.Loading) {
                    SectionState.Loading -> {
                        Loading(Modifier.fillMaxWidth().height(height))
                    }

                    SectionState.Failed -> {
                        ErrorMessage(onRetry = onRetry, modifier = Modifier.fillMaxWidth().height(height))
                    }

                    is SectionState.Loaded -> {
                        if (section == BrowseSection.PopularNew) {
                            HeroCarousel(sectionState.items, state.languages, "browse:${section.name}", onOpenManga)
                        } else {
                            MangaRow(sectionState.items, state.languages, "browse:${section.name}", onOpenManga)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HeroCarousel(
    items: List<Manga>,
    languages: List<String>,
    scope: String,
    onOpenManga: (mangaId: String, coverScope: String) -> Unit,
) {
    HorizontalCenteredHeroCarousel(
        state = rememberCarouselState { items.size },
        maxItemWidth = HeroHeight * 2 / 3,
        itemSpacing = 8.dp,
        contentPadding = PaddingValues(horizontal = 16.dp),
        modifier = Modifier.fillMaxWidth().height(HeroHeight),
    ) { index ->
        val manga = items[index]
        val title = manga.displayTitle(languages)
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .maskClip(MaterialTheme.shapes.extraLarge)
                    .clickable { onOpenManga(manga.id, scope) }
                    .testTag("hero-$index"),
        ) {
            AsyncImage(
                model = manga.coverUrl(Covers.Size.Medium),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .sharedElement(SharedKeys.cover(manga.id, scope))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            )
            // Scrim so the title stays legible on any cover; fades with the item as it narrows.
            Box(
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))))
                        .padding(start = 16.dp, end = 16.dp, top = 32.dp, bottom = 16.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier =
                        Modifier.graphicsLayer {
                            val info = carouselItemDrawInfo
                            val range = (info.maxSize - info.minSize).coerceAtLeast(1f)
                            alpha = ((info.size - info.minSize) / range).coerceIn(0f, 1f)
                        },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MangaRow(
    items: List<Manga>,
    languages: List<String>,
    scope: String,
    onOpenManga: (mangaId: String, coverScope: String) -> Unit,
) {
    HorizontalUncontainedCarousel(
        state = rememberCarouselState { items.size },
        itemWidth = CardWidth,
        itemSpacing = 12.dp,
        contentPadding = PaddingValues(horizontal = 16.dp),
        modifier = Modifier.fillMaxWidth().height(RowHeight),
    ) { index ->
        val manga = items[index]
        MangaCard(
            title = manga.displayTitle(languages),
            coverUrl = manga.coverUrl(),
            onClick = { onOpenManga(manga.id, scope) },
            modifier = Modifier.width(CardWidth),
            sharedKey = SharedKeys.cover(manga.id, scope),
        )
    }
}

@Composable
private fun SuggestionList(
    suggestions: Suggestions,
    languages: List<String>,
    onOpenManga: (mangaId: String) -> Unit,
    onSearchAll: (String) -> Unit,
) {
    when (suggestions) {
        Suggestions.Idle -> {}

        Suggestions.Loading -> {
            Loading(Modifier.fillMaxWidth().padding(32.dp))
        }

        Suggestions.Failed -> {
            Text(
                stringResource(DsR.string.ds_error_generic),
                modifier = Modifier.padding(24.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        is Suggestions.Results -> {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                if (suggestions.items.isEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.browse_suggestions_empty, suggestions.query),
                            modifier = Modifier.padding(24.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                items(suggestions.items, key = Manga::id) { manga ->
                    ListItem(
                        onClick = { onOpenManga(manga.id) },
                        supportingContent =
                            manga.authors.takeIf { it.isNotEmpty() }?.let {
                                {
                                    Text(
                                        it.joinToString()
                                    )
                                }
                            },
                        leadingContent = {
                            MangaCover(manga.coverUrl(), contentDescription = null, modifier = Modifier.width(40.dp))
                        },
                    ) { Text(manga.displayTitle(languages), maxLines = 2, overflow = TextOverflow.Ellipsis) }
                }
                item {
                    ListItem(
                        onClick = { onSearchAll(suggestions.query) },
                        leadingContent = { Icon(painterResource(DsR.drawable.ic_search), contentDescription = null) },
                    ) { Text(stringResource(R.string.browse_suggestions_search_all, suggestions.query)) }
                }
            }
        }
    }
}

@get:StringRes
private val BrowseSection.title: Int
    get() =
        when (this) {
            BrowseSection.PopularNew -> R.string.browse_section_popular_new
            BrowseSection.LatestUpdates -> R.string.browse_section_latest
            BrowseSection.RecentlyAdded -> R.string.browse_section_recent
            BrowseSection.MostFollowed -> R.string.browse_section_followed
            BrowseSection.TopRated -> R.string.browse_section_rated
        }

/** Suggestions live in the search bar's own window; their covers can't fly out of it. */
private const val SUGGESTIONS_SCOPE = "suggestions"

private val HeroHeight = 320.dp
private val CardWidth = 128.dp

// Cover (2:3 of the card width) plus two lines of title.
private val RowHeight = 248.dp
