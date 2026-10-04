package pt.aguiarvieira.m3mangadex.feature.search

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import pt.aguiarvieira.m3mangadex.core.designsystem.component.ErrorMessage
import pt.aguiarvieira.m3mangadex.core.designsystem.component.Loading
import pt.aguiarvieira.m3mangadex.core.model.ContentRating
import pt.aguiarvieira.m3mangadex.core.model.Demographic
import pt.aguiarvieira.m3mangadex.core.model.Languages
import pt.aguiarvieira.m3mangadex.core.model.MangaFilter
import pt.aguiarvieira.m3mangadex.core.model.PublicationStatus
import pt.aguiarvieira.m3mangadex.core.model.TagGroup
import pt.aguiarvieira.m3mangadex.core.designsystem.R as DsR

private val OriginalLanguages = listOf("ja", "ko", "zh", "en")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FilterSheet(
    filter: MangaFilter,
    tags: TagsState,
    onlyMyLanguages: Boolean,
    onFilterChange: ((MangaFilter) -> MangaFilter) -> Unit,
    onCycleTag: (String) -> Unit,
    onOnlyMyLanguagesChange: (Boolean) -> Unit,
    onReset: () -> Unit,
    onRetryTags: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState =
            rememberBottomSheetState(
                SheetValue.Hidden,
                enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
            )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.search_filters),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f)
            )
            OutlinedButton(onClick = onReset) { Text(stringResource(R.string.search_reset)) }
            Button(onClick = onDismiss) { Text(stringResource(R.string.search_done)) }
        }
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            item {
                ListItem(
                    onClick = { onOnlyMyLanguagesChange(!onlyMyLanguages) },
                    trailingContent = { Switch(checked = onlyMyLanguages, onCheckedChange = null) },
                ) { Text(stringResource(R.string.search_only_my_languages)) }
            }
            chipSection(R.string.search_status, PublicationStatus.entries, filter.status, { it.label }) { status ->
                onFilterChange { it.copy(status = it.status.toggle(status)) }
            }
            chipSection(
                R.string.search_demographic,
                Demographic.entries,
                filter.demographic,
                { it.label }
            ) { demographic ->
                onFilterChange { it.copy(demographic = it.demographic.toggle(demographic)) }
            }
            chipSection(
                R.string.search_content_rating,
                ContentRating.Selectable,
                filter.contentRating,
                { it.label }
            ) { rating ->
                onFilterChange { it.copy(contentRating = it.contentRating.toggle(rating)) }
            }
            item { SectionTitle(R.string.search_original_language) }
            item {
                val locale = LocalConfiguration.current.locales[0]
                ChipRow {
                    OriginalLanguages.forEach { code ->
                        FilterChip(
                            selected = code in filter.originalLanguage,
                            onClick = {
                                onFilterChange {
                                    it.copy(
                                        originalLanguage = it.originalLanguage.toggle(code)
                                    )
                                }
                            },
                            label = { Text(Languages.displayName(code, locale)) },
                        )
                    }
                }
            }
            tagSections(tags, filter, onCycleTag, onRetryTags)
        }
    }
}

private fun <T> LazyListScope.chipSection(
    @StringRes title: Int,
    options: List<T>,
    selected: Set<T>,
    label: (T) -> Int,
    onToggle: (T) -> Unit,
) {
    item { SectionTitle(title) }
    item {
        ChipRow {
            options.forEach { option ->
                FilterChip(
                    selected = option in selected,
                    onClick = { onToggle(option) },
                    label = { Text(stringResource(label(option))) },
                )
            }
        }
    }
}

private fun LazyListScope.tagSections(
    tags: TagsState,
    filter: MangaFilter,
    onCycleTag: (String) -> Unit,
    onRetryTags: () -> Unit,
) {
    when (tags) {
        TagsState.Loading -> {
            item { Loading(Modifier.fillMaxWidth().padding(24.dp)) }
        }

        TagsState.Failed -> {
            item { ErrorMessage(onRetry = onRetryTags, message = stringResource(R.string.search_tags_failed)) }
        }

        is TagsState.Loaded -> {
            item {
                Text(
                    stringResource(R.string.search_tags_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            tags.tags.groupBy { it.group }.toSortedMap().forEach { (group, groupTags) ->
                item { SectionTitle(group.label) }
                item {
                    ChipRow {
                        groupTags.forEach { tag ->
                            TagChip(
                                name = tag.name,
                                included = tag.id in filter.includedTags,
                                excluded = tag.id in filter.excludedTags,
                                onClick = { onCycleTag(tag.id) },
                            )
                        }
                    }
                }
            }
        }
    }
    item { Row(Modifier.padding(bottom = 32.dp)) {} }
}

/** Included tags read as selected; excluded ones use the error colours and a minus sign. */
@Composable
private fun TagChip(
    name: String,
    included: Boolean,
    excluded: Boolean,
    onClick: () -> Unit,
) {
    val description =
        when {
            included -> stringResource(R.string.tag_included, name)
            excluded -> stringResource(R.string.tag_excluded, name)
            else -> name
        }
    FilterChip(
        selected = included || excluded,
        onClick = onClick,
        label = { Text(name) },
        leadingIcon =
            when {
                included -> {
                    { Icon(painterResource(DsR.drawable.ic_check), contentDescription = null) }
                }

                excluded -> {
                    { Icon(painterResource(DsR.drawable.ic_remove), contentDescription = null) }
                }

                else -> {
                    null
                }
            },
        colors =
            if (excluded) {
                FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.onErrorContainer,
                )
            } else {
                FilterChipDefaults.filterChipColors()
            },
        modifier = Modifier.semantics { contentDescription = description },
    )
}

@Composable
private fun SectionTitle(
    @StringRes title: Int,
) {
    Text(
        stringResource(title),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun ChipRow(content: @Composable () -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) { content() }
}

private fun <T> Set<T>.toggle(value: T): Set<T> = if (value in this) this - value else this + value

private val PublicationStatus.label: Int
    get() =
        when (this) {
            PublicationStatus.Ongoing -> R.string.status_ongoing
            PublicationStatus.Completed -> R.string.status_completed
            PublicationStatus.Hiatus -> R.string.status_hiatus
            PublicationStatus.Cancelled -> R.string.status_cancelled
        }

private val Demographic.label: Int
    get() =
        when (this) {
            Demographic.Shounen -> R.string.demographic_shounen
            Demographic.Shoujo -> R.string.demographic_shoujo
            Demographic.Seinen -> R.string.demographic_seinen
            Demographic.Josei -> R.string.demographic_josei
        }

private val ContentRating.label: Int
    get() =
        when (this) {
            ContentRating.Safe -> R.string.rating_safe
            ContentRating.Suggestive -> R.string.rating_suggestive
            ContentRating.Erotica, ContentRating.Pornographic -> R.string.rating_erotica
        }

private val TagGroup.label: Int
    get() =
        when (this) {
            TagGroup.Genre -> R.string.search_tag_group_genre
            TagGroup.Theme -> R.string.search_tag_group_theme
            TagGroup.Format -> R.string.search_tag_group_format
            TagGroup.Content -> R.string.search_tag_group_content
        }
