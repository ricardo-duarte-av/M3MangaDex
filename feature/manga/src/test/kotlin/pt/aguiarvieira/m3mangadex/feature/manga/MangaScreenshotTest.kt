package pt.aguiarvieira.m3mangadex.feature.manga

import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import pt.aguiarvieira.m3mangadex.core.designsystem.theme.M3MangaDexTheme
import pt.aguiarvieira.m3mangadex.core.model.Chapter
import pt.aguiarvieira.m3mangadex.core.model.ChapterProgress
import pt.aguiarvieira.m3mangadex.core.model.ContentRating
import pt.aguiarvieira.m3mangadex.core.model.Manga
import pt.aguiarvieira.m3mangadex.core.model.MangaStats
import pt.aguiarvieira.m3mangadex.core.model.PublicationStatus
import pt.aguiarvieira.m3mangadex.core.model.ReadingStatus
import pt.aguiarvieira.m3mangadex.core.model.ScanlationGroup
import pt.aguiarvieira.m3mangadex.core.model.Tag
import pt.aguiarvieira.m3mangadex.core.model.TagGroup
import java.time.Instant

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class MangaScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    private val chapters =
        listOf(
            Chapter("c3", "m1", "2", "12", "The Altar of Life", "en", 30, null, null, listOf(ScanlationGroup("g", "Team A", false))),
            Chapter("c2", "m1", "1", "2", "Wolf Slayer", "en", 0, "https://example.org", null, listOf(ScanlationGroup("w", "Webnovel", true))),
            Chapter("c1", "m1", "1", "1", null, "en", 25, null, null, emptyList()),
        )

    @Test
    fun loaded() {
        val state =
            MangaUiState.Loaded(
                manga = sampleManga(),
                stats = MangaStats(follows = 320_541, rating = 9.33),
                chapters = ChaptersState.Loaded(groupByVolume(chapters), chapters),
                languages = listOf("en"),
            )
        // Chapter 1 finished, chapter 12 half-way: the button continues it.
        val progress =
            mapOf(
                "c1" to ChapterProgress("c1", "m1", "1", page = 24, pageCount = 25, readAt = Instant.EPOCH),
                "c3" to ChapterProgress("c3", "m1", "12", page = 9, pageCount = 30, readAt = Instant.EPOCH),
            )
        val reading = ReadingState(progress, resume = chapters[0], started = true)
        compose.mainClock.autoAdvance = false
        compose.setContent {
            M3MangaDexTheme(darkTheme = true, dynamicColor = false) {
                MangaScreen(
                    state = state,
                    snackbar = SnackbarHostState(),
                    onBack = {},
                    onRetry = {},
                    onOpenTag = {},
                    onOpenChapter = {},
                    reading = reading,
                    otherLanguages = OtherLanguages(available = listOf("es-la", "pl"), selected = setOf("pl")),
                    onToggleLanguage = {},
                    account = AccountState(loggedIn = true, status = ReadingStatus.Reading, following = true),
                    onSetStatus = {},
                    onToggleFollow = {},
                    onToggleRead = {},
                )
            }
        }
        compose.onRoot().captureRoboImage("src/test/screenshots/manga_loaded.png")
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
