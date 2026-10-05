package pt.aguiarvieira.m3mangadex.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.rememberNavigationSuiteScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.SinglePaneSceneStrategy
import androidx.navigation3.ui.NavDisplay
import kotlinx.serialization.Serializable
import pt.aguiarvieira.m3mangadex.BuildConfig
import pt.aguiarvieira.m3mangadex.R
import pt.aguiarvieira.m3mangadex.core.data.BrowseSection
import pt.aguiarvieira.m3mangadex.core.designsystem.theme.CoverHolder
import pt.aguiarvieira.m3mangadex.core.designsystem.theme.CoverTheme
import pt.aguiarvieira.m3mangadex.core.designsystem.theme.LocalCoverHolder
import pt.aguiarvieira.m3mangadex.feature.browse.BrowseRoute
import pt.aguiarvieira.m3mangadex.feature.library.LibraryRoute
import pt.aguiarvieira.m3mangadex.feature.login.LoginRoute
import pt.aguiarvieira.m3mangadex.feature.manga.MangaRoute
import pt.aguiarvieira.m3mangadex.feature.reader.ReaderRoute
import pt.aguiarvieira.m3mangadex.feature.search.SearchArgs
import pt.aguiarvieira.m3mangadex.feature.search.SearchRoute
import pt.aguiarvieira.m3mangadex.feature.settings.SettingsRoute
import pt.aguiarvieira.m3mangadex.feature.updates.UpdatesRoute

@Serializable data object BrowseKey : NavKey

@Serializable data object LibraryKey : NavKey

@Serializable data object UpdatesKey : NavKey

@Serializable data object SettingsKey : NavKey

/** Full search: a typed query, a Browse row's "See all" ([section]), or a tag from a manga. */
@Serializable data class SearchKey(
    val query: String = "",
    val section: String? = null,
    val tagId: String? = null,
) : NavKey

@Serializable data class MangaKey(
    val mangaId: String,
) : NavKey

@Serializable data object LoginKey : NavKey

/** Full screen, over everything: the navigation bar hides while it's on top. */
@Serializable data class ReaderKey(
    val mangaId: String,
    val chapterId: String,
) : NavKey

/** The top-level destinations, one per navigation-suite item, each with its own back stack. */
enum class TopLevel(
    val key: NavKey,
    @param:StringRes val label: Int,
    @param:DrawableRes val icon: Int,
) {
    Browse(BrowseKey, R.string.tab_browse, R.drawable.ic_browse),
    Library(LibraryKey, R.string.tab_library, R.drawable.ic_library),
    Updates(UpdatesKey, R.string.tab_updates, R.drawable.ic_updates),
    Settings(SettingsKey, R.string.tab_settings, R.drawable.ic_settings),
}

/**
 * The app shell. [NavigationSuiteScaffold] picks a bottom bar, rail or drawer from the window size;
 * inside, each tab's [NavDisplay] uses the list-detail strategy, so on large screens a list sits
 * beside its detail (manga details, the reader's chapter list) instead of replacing it.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun M3MangaDexApp(modifier: Modifier = Modifier) {
    var current by rememberSaveable { mutableStateOf(TopLevel.Browse) }
    val stacks =
        TopLevel.entries.associateWith { tab -> rememberNavBackStack(tab.key) }
    val listDetail = rememberListDetailSceneStrategy<NavKey>()
    val suiteState = rememberNavigationSuiteScaffoldState()
    val reading = stacks.getValue(current).lastOrNull() is ReaderKey
    LaunchedEffect(reading) { if (reading) suiteState.hide() else suiteState.show() }

    // A manga's screens publish its cover; the whole shell (navigation bar included) wears it.
    val covers = remember { CoverHolder() }
    CompositionLocalProvider(LocalCoverHolder provides covers) {
        CoverTheme(coverUrl = covers.current) {
            NavigationSuiteScaffold(
                modifier = modifier,
                state = suiteState,
                navigationSuiteItems = {
                    TopLevel.entries.forEach { tab ->
                        item(
                            selected = tab == current,
                            onClick = {
                                // Re-selecting the current tab pops it back to its root.
                                if (tab == current) stacks.getValue(tab).retainRoot() else current = tab
                            },
                            icon = { Icon(painterResource(tab.icon), contentDescription = null) },
                            label = { Text(stringResource(tab.label)) },
                        )
                    }
                },
            ) {
                val backStack = stacks.getValue(current)
                NavDisplay(
                    backStack = backStack,
                    onBack = { backStack.removeLastOrNull() },
                    entryDecorators =
                        listOf(
                            rememberSaveableStateHolderNavEntryDecorator(),
                            rememberViewModelStoreNavEntryDecorator(),
                        ),
                    sceneStrategies = listOf(listDetail, SinglePaneSceneStrategy()),
                    entryProvider =
                        entryProvider {
                            entry<BrowseKey>(metadata = listPane()) {
                                BrowseRoute(
                                    onOpenManga = backStack::openManga,
                                    onOpenSearch = { query, section -> backStack.add(SearchKey(query, section?.name)) },
                                )
                            }
                            entry<SearchKey>(metadata = listPane()) { key ->
                                SearchRoute(
                                    args = SearchArgs(key.query, key.section?.let(BrowseSection::valueOf), key.tagId),
                                    onBack = { backStack.removeLastOrNull() },
                                    onOpenManga = backStack::openManga,
                                )
                            }
                            entry<MangaKey>(metadata = ListDetailSceneStrategy.detailPane()) { key ->
                                MangaRoute(
                                    mangaId = key.mangaId,
                                    onBack = { backStack.removeLastOrNull() },
                                    onOpenTag = { backStack.add(SearchKey(tagId = it)) },
                                    onReadChapter = { chapter -> backStack.add(ReaderKey(key.mangaId, chapter.id)) },
                                )
                            }
                            entry<ReaderKey> { key ->
                                ReaderRoute(
                                    mangaId = key.mangaId,
                                    chapterId = key.chapterId,
                                    onBack = { backStack.removeLastOrNull() },
                                    // Chapter to chapter replaces the reader, so back still leads to the details.
                                    onOpenChapter = { chapterId ->
                                        backStack[backStack.lastIndex] =
                                            ReaderKey(key.mangaId, chapterId)
                                    },
                                )
                            }
                            entry<LibraryKey>(metadata = listPane()) {
                                LibraryRoute(onOpenManga = backStack::openManga, onLogin = { backStack.add(LoginKey) })
                            }
                            entry<UpdatesKey> {
                                UpdatesRoute(
                                    onReadChapter = {
                                        mangaId,
                                        chapterId,
                                        ->
                                        backStack.add(ReaderKey(mangaId, chapterId))
                                    },
                                    onLogin = { backStack.add(LoginKey) },
                                )
                            }
                            entry<SettingsKey> {
                                SettingsRoute(
                                    versionName = BuildConfig.VERSION_NAME,
                                    onLogin = { backStack.add(LoginKey) }
                                )
                            }
                            entry<LoginKey> {
                                LoginRoute(
                                    onBack = { backStack.removeLastOrNull() },
                                    onLoggedIn = { backStack.remove(LoginKey) },
                                )
                            }
                        },
                )
            }
        }
    }
}

/**
 * Opens a manga. If one is already open on top (beside the list on a large screen) it is replaced
 * rather than stacked, so back returns to the list instead of walking through every title tapped.
 */
private fun MutableList<NavKey>.openManga(mangaId: String) {
    val key = MangaKey(mangaId)
    when (lastOrNull()) {
        key -> Unit
        is MangaKey -> set(lastIndex, key)
        else -> add(key)
    }
}

private fun MutableList<NavKey>.retainRoot() {
    while (size > 1) removeAt(lastIndex)
}

/** A list pane whose empty detail side (large screens only) shows a hint instead of nothing. */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
private fun listPane() =
    ListDetailSceneStrategy.listPane(
        detailPlaceholder = {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    stringResource(R.string.placeholder_detail),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    )
