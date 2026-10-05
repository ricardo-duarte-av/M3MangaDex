package pt.aguiarvieira.m3mangadex.feature.updates

import android.content.ActivityNotFoundException
import android.text.format.DateUtils
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import pt.aguiarvieira.m3mangadex.core.designsystem.component.ErrorMessage
import pt.aguiarvieira.m3mangadex.core.designsystem.component.Loading
import pt.aguiarvieira.m3mangadex.core.designsystem.component.LoginPrompt
import pt.aguiarvieira.m3mangadex.core.designsystem.component.MangaCover
import pt.aguiarvieira.m3mangadex.core.model.Chapter
import pt.aguiarvieira.m3mangadex.core.model.FeedEntry
import pt.aguiarvieira.m3mangadex.core.model.coverUrl
import pt.aguiarvieira.m3mangadex.core.designsystem.R as DsR

/** [onReadChapter] opens a MangaDex-hosted chapter in the reader; publisher-hosted ones open in a Custom Tab. */
@Composable
fun UpdatesRoute(
    onReadChapter: (mangaId: String, chapterId: String) -> Unit,
    onLogin: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: UpdatesViewModel = hiltViewModel(),
) {
    val loggedIn by viewModel.loggedIn.collectAsStateWithLifecycle()
    val languages by viewModel.languages.collectAsStateWithLifecycle()
    val updates = viewModel.updates.collectAsLazyPagingItems()
    val context = LocalContext.current
    UpdatesScreen(
        loggedIn = loggedIn,
        updates = updates,
        languages = languages,
        onOpenChapter = { mangaId, chapter ->
            val external = chapter.externalUrl
            if (chapter.isExternal && external != null) {
                try {
                    CustomTabsIntent
                        .Builder()
                        .setShowTitle(true)
                        .build()
                        .launchUrl(context, external.toUri())
                } catch (_: ActivityNotFoundException) {
                    // No browser at all; nothing sensible to do.
                }
            } else {
                onReadChapter(mangaId, chapter.id)
            }
        },
        onLogin = onLogin,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun UpdatesScreen(
    loggedIn: Boolean,
    updates: LazyPagingItems<FeedEntry>,
    languages: List<String>,
    onOpenChapter: (mangaId: String, chapter: Chapter) -> Unit,
    onLogin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier, topBar = {
        MediumFlexibleTopAppBar(title = { Text(stringResource(R.string.updates_title)) })
    }) { padding ->
        val content = Modifier.fillMaxSize().padding(top = padding.calculateTopPadding())
        val refresh = updates.loadState.refresh
        when {
            !loggedIn -> {
                Box(content, contentAlignment = Alignment.Center) {
                    LoginPrompt(
                        stringResource(R.string.updates_logged_out_title),
                        stringResource(R.string.updates_logged_out_body),
                        onLogin,
                    )
                }
            }

            updates.itemCount == 0 && refresh is LoadState.Loading -> {
                Loading(content)
            }

            updates.itemCount == 0 && refresh is LoadState.Error -> {
                Box(content, contentAlignment = Alignment.Center) { ErrorMessage(onRetry = updates::retry) }
            }

            else -> {
                PullToRefreshBox(
                    isRefreshing = refresh is LoadState.Loading,
                    onRefresh = updates::refresh,
                    modifier = content
                ) {
                    LazyColumn(Modifier.fillMaxSize()) {
                        if (updates.itemCount == 0) {
                            item {
                                Text(
                                    stringResource(R.string.updates_empty),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                                )
                            }
                        }
                        // No keys: the feed can repeat a chapter across pages as new ones arrive.
                        items(count = updates.itemCount) { index ->
                            updates[index]?.let { entry -> UpdateRow(entry, languages, onOpenChapter) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UpdateRow(
    entry: FeedEntry,
    languages: List<String>,
    onOpenChapter: (mangaId: String, chapter: Chapter) -> Unit,
) {
    val chapter = entry.chapter
    val mangaId = chapter.mangaId ?: return
    val number =
        chapter.number?.let { stringResource(R.string.updates_chapter, it) } ?: stringResource(R.string.updates_oneshot)
    val whenText =
        chapter.readableAt?.let {
            DateUtils
                .getRelativeTimeSpanString(
                    it.toEpochMilli(),
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS
                ).toString()
        }
    ListItem(
        onClick = { onOpenChapter(mangaId, chapter) },
        leadingContent = {
            MangaCover(
                entry.manga?.coverUrl(),
                contentDescription = null,
                modifier = Modifier.width(48.dp)
            )
        },
        supportingContent = {
            Text(
                listOfNotNull(
                    listOfNotNull(number, chapter.title).joinToString(" · "),
                    chapter.groups.joinToString {
                        it.name
                    },
                    whenText
                ).filter(String::isNotBlank)
                    .joinToString(" · "),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        },
        trailingContent =
            if (chapter.isExternal) {
                { Icon(painterResource(DsR.drawable.ic_open_in_new), contentDescription = null) }
            } else {
                null
            },
    ) {
        Text(entry.manga?.displayTitle(languages).orEmpty(), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
