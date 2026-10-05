package pt.aguiarvieira.m3mangadex.feature.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pt.aguiarvieira.m3mangadex.core.designsystem.component.Loading
import pt.aguiarvieira.m3mangadex.core.model.ContentRating
import pt.aguiarvieira.m3mangadex.core.model.Languages
import pt.aguiarvieira.m3mangadex.core.model.UserPreferences
import pt.aguiarvieira.m3mangadex.core.designsystem.R as DsR

@Composable
fun SettingsRoute(
    versionName: String,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SettingsScreen(
        preferences = state,
        versionName = versionName,
        onToggleLanguage = viewModel::toggleLanguage,
        onToggleRating = viewModel::toggleRating,
        onDataSaverChange = viewModel::setDataSaver,
        onSpreadsChange = viewModel::setDoublePageSpreads,
        onVolumeKeysChange = viewModel::setVolumeKeyPaging,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsScreen(
    preferences: UserPreferences?,
    versionName: String,
    onToggleLanguage: (String) -> Unit,
    onToggleRating: (ContentRating) -> Unit,
    onDataSaverChange: (Boolean) -> Unit,
    onSpreadsChange: (Boolean) -> Unit,
    onVolumeKeysChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val uriHandler = LocalUriHandler.current
    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediumFlexibleTopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                scrollBehavior = scrollBehavior
            )
        },
    ) { padding ->
        if (preferences == null) {
            Loading(Modifier.fillMaxSize().padding(padding))
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = 24.dp)
        ) {
            item { Heading(R.string.settings_languages) }
            item { Hint(R.string.settings_languages_hint) }
            item {
                val locale = LocalConfiguration.current.locales[0]
                // Selected first (in priority order), then the common ones not chosen yet.
                val options = preferences.chapterLanguages + (Languages.common - preferences.chapterLanguages.toSet())
                Chips {
                    options.forEach { code ->
                        FilterChip(
                            selected = code in preferences.chapterLanguages,
                            onClick = { onToggleLanguage(code) },
                            label = { Text(Languages.displayName(code, locale)) },
                        )
                    }
                }
            }
            item { Heading(R.string.settings_content) }
            item { Hint(R.string.settings_content_hint) }
            item {
                Chips {
                    ContentRating.Selectable.forEach { rating ->
                        FilterChip(
                            selected = rating in preferences.contentRatings,
                            onClick = { onToggleRating(rating) },
                            label = { Text(stringResource(rating.label)) },
                        )
                    }
                }
            }
            item { Heading(R.string.settings_reading) }
            item {
                ListItem(
                    onClick = { onDataSaverChange(!preferences.dataSaver) },
                    supportingContent = { Text(stringResource(R.string.settings_data_saver_hint)) },
                    trailingContent = { Switch(checked = preferences.dataSaver, onCheckedChange = null) },
                ) { Text(stringResource(R.string.settings_data_saver)) }
            }
            item {
                ListItem(
                    onClick = { onSpreadsChange(!preferences.doublePageSpreads) },
                    supportingContent = { Text(stringResource(R.string.settings_spreads_hint)) },
                    trailingContent = { Switch(checked = preferences.doublePageSpreads, onCheckedChange = null) },
                ) { Text(stringResource(R.string.settings_spreads)) }
            }
            item {
                ListItem(
                    onClick = { onVolumeKeysChange(!preferences.volumeKeyPaging) },
                    trailingContent = { Switch(checked = preferences.volumeKeyPaging, onCheckedChange = null) },
                ) { Text(stringResource(R.string.settings_volume_keys)) }
            }
            item { Heading(R.string.settings_about) }
            item {
                ListItem(supportingContent = { Text(stringResource(R.string.settings_credit)) }) {
                    Text(stringResource(R.string.settings_version, versionName))
                }
            }
            item {
                ListItem(
                    onClick = { uriHandler.openUri(MANGADEX_URL) },
                    trailingContent = { Icon(painterResource(DsR.drawable.ic_open_in_new), contentDescription = null) },
                ) { Text(stringResource(R.string.settings_mangadex)) }
            }
            item {
                ListItem(
                    onClick = { uriHandler.openUri(SOURCE_URL) },
                    trailingContent = { Icon(painterResource(DsR.drawable.ic_open_in_new), contentDescription = null) },
                ) { Text(stringResource(R.string.settings_source)) }
            }
        }
    }
}

@Composable
private fun Heading(
    @StringRes text: Int,
) {
    Text(
        stringResource(text),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 4.dp),
    )
}

@Composable
private fun Hint(
    @StringRes text: Int,
) {
    Text(
        stringResource(text),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
    )
}

@Composable
private fun Chips(content: @Composable () -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) { content() }
}

private val ContentRating.label: Int
    get() =
        when (this) {
            ContentRating.Safe -> R.string.settings_rating_safe
            ContentRating.Suggestive -> R.string.settings_rating_suggestive
            ContentRating.Erotica, ContentRating.Pornographic -> R.string.settings_rating_erotica
        }

private const val MANGADEX_URL = "https://mangadex.org"
private const val SOURCE_URL = "https://github.com/ricardo-duarte-av/M3MangaDex"
