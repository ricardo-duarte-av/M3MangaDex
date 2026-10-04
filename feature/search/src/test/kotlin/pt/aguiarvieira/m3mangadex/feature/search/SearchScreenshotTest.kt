package pt.aguiarvieira.m3mangadex.feature.search

import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import pt.aguiarvieira.m3mangadex.core.designsystem.theme.M3MangaDexTheme
import pt.aguiarvieira.m3mangadex.core.model.ContentRating
import pt.aguiarvieira.m3mangadex.core.model.Manga
import pt.aguiarvieira.m3mangadex.core.model.MangaFilter
import pt.aguiarvieira.m3mangadex.core.model.PublicationStatus
import pt.aguiarvieira.m3mangadex.core.model.Tag
import pt.aguiarvieira.m3mangadex.core.model.TagGroup

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class SearchScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun results() {
        val items = List(9) { sampleManga(id = "m$it", title = "Title number ${it + 1}") }
        compose.setContent {
            M3MangaDexTheme(darkTheme = true, dynamicColor = false) {
                SearchScreen(
                    query = rememberTextFieldState("solo"),
                    filter = MangaFilter(status = setOf(PublicationStatus.Completed)),
                    results = flowOf(PagingData.from(items)).collectAsLazyPagingItems(),
                    languages = listOf("en"),
                    tags = TagsState.Loading,
                    onlyMyLanguages = true,
                    onFilterChange = {},
                    onCycleTag = {},
                    onOnlyMyLanguagesChange = {},
                    onResetFilters = {},
                    onRetryTags = {},
                    onBack = {},
                    onOpenManga = {},
                )
            }
        }
        compose.onRoot().captureRoboImage("src/test/screenshots/search_results.png")
    }
}

internal fun sampleManga(
    id: String = "m1",
    title: String = "Solo Leveling",
) = Manga(
    id = id,
    title = mapOf("en" to title),
    altTitles = emptyList(),
    description = mapOf("en" to "10 years ago, after **the Gate** that connected the real world with the monster world opened, some of the ordinary, everyday people received the power to hunt monsters within the Gate. They are known as Hunters. However, not all Hunters are powerful."),
    originalLanguage = "ko",
    status = PublicationStatus.Completed,
    demographic = null,
    contentRating = ContentRating.Safe,
    year = 2018,
    tags = listOf(Tag("t1", "Action", TagGroup.Genre), Tag("t2", "Fantasy", TagGroup.Genre), Tag("t3", "Monsters", TagGroup.Theme)),
    authors = listOf("Chugong"),
    artists = listOf("DUBU (REDICE Studio)"),
    coverFileName = null,
    availableLanguages = listOf("en"),
    lastVolume = null,
    lastChapter = "200",
    updatedAt = null,
)
