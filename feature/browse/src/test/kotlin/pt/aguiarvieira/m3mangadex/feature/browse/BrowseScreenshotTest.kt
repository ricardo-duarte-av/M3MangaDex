package pt.aguiarvieira.m3mangadex.feature.browse

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import pt.aguiarvieira.m3mangadex.core.data.BrowseSection
import pt.aguiarvieira.m3mangadex.core.designsystem.theme.M3MangaDexTheme
import pt.aguiarvieira.m3mangadex.core.model.ContentRating
import pt.aguiarvieira.m3mangadex.core.model.Manga
import pt.aguiarvieira.m3mangadex.core.model.PublicationStatus
import pt.aguiarvieira.m3mangadex.core.model.Tag
import pt.aguiarvieira.m3mangadex.core.model.TagGroup

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class BrowseScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun sections() {
        val titles = listOf("Solo Leveling", "My Dress-Up Darling", "Frieren: Beyond Journey's End", "Dandadan", "Blue Lock")
        val items = titles.mapIndexed { i, title -> sampleManga(id = "m$i", title = title) }
        val state =
            BrowseUiState(
                sections =
                    BrowseSection.entries.associateWith {
                        if (it == BrowseSection.TopRated) SectionState.Failed else SectionState.Loaded(items)
                    },
                languages = listOf("en"),
            )
        compose.mainClock.autoAdvance = false
        compose.setContent {
            M3MangaDexTheme(darkTheme = false, dynamicColor = false) {
                BrowseScreen(state, Suggestions.Idle, onQueryChange = {}, onRefresh = {}, onOpenManga = { _, _ -> }, onOpenSearch = { _, _ -> })
            }
        }
        compose.onRoot().captureRoboImage("src/test/screenshots/browse.png")
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
