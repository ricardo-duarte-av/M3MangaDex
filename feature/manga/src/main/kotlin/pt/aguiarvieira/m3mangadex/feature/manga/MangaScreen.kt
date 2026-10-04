package pt.aguiarvieira.m3mangadex.feature.manga

import android.content.ActivityNotFoundException
import android.content.Context
import android.icu.text.CompactDecimalFormat
import android.text.format.DateUtils
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import pt.aguiarvieira.m3mangadex.core.designsystem.component.ErrorMessage
import pt.aguiarvieira.m3mangadex.core.designsystem.component.Loading
import pt.aguiarvieira.m3mangadex.core.designsystem.component.MangaCover
import pt.aguiarvieira.m3mangadex.core.model.Chapter
import pt.aguiarvieira.m3mangadex.core.model.Covers
import pt.aguiarvieira.m3mangadex.core.model.Descriptions
import pt.aguiarvieira.m3mangadex.core.model.Languages
import pt.aguiarvieira.m3mangadex.core.model.Manga
import pt.aguiarvieira.m3mangadex.core.model.MangaStats
import pt.aguiarvieira.m3mangadex.core.model.PublicationStatus
import pt.aguiarvieira.m3mangadex.core.model.coverUrl
import pt.aguiarvieira.m3mangadex.core.designsystem.R as DsR

/**
 * A manga's details and chapter list. [onReadChapter] opens a MangaDex-hosted chapter (null until
 * the reader exists); chapters hosted by publishers always open in a Custom Tab.
 */
@Composable
fun MangaRoute(
    mangaId: String,
    onBack: () -> Unit,
    onOpenTag: (String) -> Unit,
    modifier: Modifier = Modifier,
    onReadChapter: ((Chapter) -> Unit)? = null,
    viewModel: MangaViewModel =
        hiltViewModel<MangaViewModel, MangaViewModel.Factory>(key = "manga:$mangaId") { it.create(mangaId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val readerComing = stringResource(R.string.manga_reader_coming)
    MangaScreen(
        state = state,
        snackbar = snackbar,
        onBack = onBack,
        onRetry = viewModel::retry,
        onOpenTag = onOpenTag,
        onOpenChapter = { chapter ->
            when {
                chapter.isExternal -> openExternal(context, chapter.externalUrl!!)
                onReadChapter != null -> onReadChapter(chapter)
                else -> scope.launch { snackbar.showSnackbar(readerComing) }
            }
        },
        modifier = modifier,
    )
}

private fun openExternal(
    context: Context,
    url: String,
) {
    try {
        CustomTabsIntent
            .Builder()
            .setShowTitle(true)
            .build()
            .launchUrl(context, url.toUri())
    } catch (_: ActivityNotFoundException) {
        // No browser at all; nothing sensible to do.
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MangaScreen(
    state: MangaUiState,
    snackbar: SnackbarHostState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onOpenTag: (String) -> Unit,
    onOpenChapter: (Chapter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    if (state is MangaUiState.Loaded) {
                        Text(state.manga.displayTitle(state.languages), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(DsR.drawable.ic_arrow_back), stringResource(DsR.string.ds_back))
                    }
                },
            )
        },
    ) { padding ->
        when (state) {
            MangaUiState.Loading -> {
                Loading(Modifier.fillMaxSize().padding(padding))
            }

            MangaUiState.Failed -> {
                Box(
                    Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) { ErrorMessage(onRetry = onRetry) }
            }

            is MangaUiState.Loaded -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding =
                        PaddingValues(
                            top = padding.calculateTopPadding(),
                            bottom =
                                padding.calculateBottomPadding() + 16.dp
                        ),
                ) {
                    item(key = "header") { Header(state.manga, state.stats, state.languages) }
                    item(key = "actions") {
                        val first = (state.chapters as? ChaptersState.Loaded)?.first
                        Button(
                            onClick = { first?.let(onOpenChapter) },
                            enabled = first != null,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        ) { Text(stringResource(R.string.manga_read)) }
                    }
                    item(key = "description") { Description(state.manga.displayDescription(state.languages)) }
                    item(key = "tags") { Tags(state.manga, onOpenTag) }
                    chapters(state.chapters, state.languages, onOpenChapter, onRetry)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun Header(
    manga: Manga,
    stats: MangaStats?,
    languages: List<String>,
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        // The cover, blurred and faded into the surface, as a backdrop.
        AsyncImage(
            model = manga.coverUrl(Covers.Size.Medium),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize().blur(32.dp).alpha(0.45f),
        )
        Box(
            Modifier
                .matchParentSize()
                .background(Brush.verticalGradient(listOf(Color.Transparent, MaterialTheme.colorScheme.surface))),
        )
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            MangaCover(
                url = manga.coverUrl(Covers.Size.Medium),
                contentDescription = null,
                modifier = Modifier.width(120.dp),
                shape = MaterialTheme.shapes.large,
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                Text(manga.displayTitle(languages), style = MaterialTheme.typography.headlineSmallEmphasized)
                val people = (manga.authors + manga.artists).distinct()
                if (people.isNotEmpty()) {
                    Text(
                        people.joinToString(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                val facts = listOfNotNull(manga.status?.let { stringResource(it.label) }, manga.year?.toString())
                if (facts.isNotEmpty()) {
                    Text(
                        facts.joinToString(" · "),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                stats?.let { StatsLine(it) }
            }
        }
    }
}

@Composable
private fun StatsLine(stats: MangaStats) {
    val locale = LocalConfiguration.current.locales[0]
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        stats.rating?.let {
            Icon(
                painterResource(DsR.drawable.ic_star),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(16.dp),
            )
            Text(String.format(locale, "%.2f", it), style = MaterialTheme.typography.labelLarge)
        }
        stats.follows?.let {
            val compact = CompactDecimalFormat.getInstance(locale, CompactDecimalFormat.CompactStyle.SHORT).format(it)
            Text(
                (if (stats.rating != null) " · " else "") + stringResource(R.string.manga_follows, compact),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Description(markdown: String) {
    if (markdown.isBlank()) return
    val text = remember(markdown) { Descriptions.plain(markdown) }
    var expanded by rememberSaveable { mutableStateOf(false) }
    var overflows by remember { mutableStateOf(false) }
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = if (expanded) Int.MAX_VALUE else COLLAPSED_LINES,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { if (!expanded) overflows = it.hasVisualOverflow },
        )
        if (overflows || expanded) {
            TextButton(onClick = { expanded = !expanded }) {
                Text(stringResource(if (expanded) R.string.manga_less else R.string.manga_more))
            }
        }
    }
}

@Composable
private fun Tags(
    manga: Manga,
    onOpenTag: (String) -> Unit,
) {
    if (manga.tags.isEmpty()) return
    FlowRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        manga.tags.forEach { tag ->
            SuggestionChip(onClick = { onOpenTag(tag.id) }, label = { Text(tag.name) })
        }
    }
}

private fun LazyListScope.chapters(
    state: ChaptersState,
    languages: List<String>,
    onOpenChapter: (Chapter) -> Unit,
    onRetry: () -> Unit,
) {
    when (state) {
        ChaptersState.Loading -> {
            item(key = "chapters-loading") { Loading(Modifier.fillMaxWidth().padding(32.dp)) }
        }

        ChaptersState.Failed -> {
            item(key = "chapters-failed") {
                ErrorMessage(
                    onRetry = onRetry,
                    message = stringResource(R.string.manga_chapters_failed),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        is ChaptersState.Loaded -> {
            item(key = "chapters-title") {
                Text(
                    stringResource(R.string.manga_chapters_count, state.count),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp),
                )
            }
            if (state.count == 0) {
                item(key = "chapters-empty") {
                    val locale = LocalConfiguration.current.locales[0]
                    Text(
                        stringResource(
                            R.string.manga_no_chapters,
                            languages.joinToString { Languages.displayName(it, locale) }
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
            state.volumes.forEach { group ->
                stickyHeader(key = "volume-${group.volume}") {
                    Surface(color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            group.volume?.let { stringResource(R.string.manga_volume, it) }
                                ?: stringResource(R.string.manga_no_volume),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                }
                items(group.chapters, key = { "chapter-${it.id}" }) { chapter ->
                    ChapterRow(chapter, showLanguage = languages.size > 1, onClick = { onOpenChapter(chapter) })
                }
            }
        }
    }
}

@Composable
private fun ChapterRow(
    chapter: Chapter,
    showLanguage: Boolean,
    onClick: () -> Unit,
) {
    val number =
        chapter.number?.let { stringResource(R.string.manga_chapter, it) } ?: stringResource(R.string.manga_oneshot)
    val headline = listOfNotNull(number, chapter.title).joinToString(" · ")
    val groups = chapter.groups.joinToString { it.name }.ifBlank { stringResource(R.string.manga_no_group) }
    val whenText =
        chapter.readableAt?.let {
            DateUtils
                .getRelativeTimeSpanString(
                    it.toEpochMilli(),
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS
                ).toString()
        }
    val supporting =
        listOfNotNull(
            chapter.language.uppercase().takeIf { showLanguage },
            groups,
            whenText,
            stringResource(R.string.manga_external).takeIf { chapter.isExternal },
        ).joinToString(" · ")
    ListItem(
        onClick = onClick,
        supportingContent = { Text(supporting, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        trailingContent =
            if (chapter.isExternal) {
                { Icon(painterResource(DsR.drawable.ic_open_in_new), contentDescription = null) }
            } else {
                null
            },
    ) { Text(headline, maxLines = 1, overflow = TextOverflow.Ellipsis) }
}

private val PublicationStatus.label: Int
    get() =
        when (this) {
            PublicationStatus.Ongoing -> R.string.manga_status_ongoing
            PublicationStatus.Completed -> R.string.manga_status_completed
            PublicationStatus.Hiatus -> R.string.manga_status_hiatus
            PublicationStatus.Cancelled -> R.string.manga_status_cancelled
        }

private const val COLLAPSED_LINES = 4
