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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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

@Serializable data object BrowseKey : NavKey

@Serializable data object LibraryKey : NavKey

@Serializable data object UpdatesKey : NavKey

@Serializable data object SettingsKey : NavKey

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

    NavigationSuiteScaffold(
        modifier = modifier,
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
                        PlaceholderScreen(R.string.tab_browse, stringResource(R.string.placeholder_browse))
                    }
                    entry<LibraryKey>(metadata = listPane()) {
                        PlaceholderScreen(R.string.tab_library, stringResource(R.string.placeholder_library))
                    }
                    entry<UpdatesKey>(metadata = listPane()) {
                        PlaceholderScreen(R.string.tab_updates, stringResource(R.string.placeholder_updates))
                    }
                    entry<SettingsKey> {
                        PlaceholderScreen(
                            R.string.tab_settings,
                            stringResource(R.string.settings_version, BuildConfig.VERSION_NAME) + "\n\n" +
                                stringResource(R.string.settings_credit),
                        )
                    }
                },
        )
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PlaceholderScreen(
    @StringRes title: Int,
    body: String,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { MediumFlexibleTopAppBar(title = { Text(stringResource(title)) }) },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                body,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
