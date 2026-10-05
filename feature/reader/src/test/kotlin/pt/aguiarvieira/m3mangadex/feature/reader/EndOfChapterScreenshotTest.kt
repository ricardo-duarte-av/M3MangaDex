package pt.aguiarvieira.m3mangadex.feature.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import pt.aguiarvieira.m3mangadex.core.model.ChapterNeighbors
import pt.aguiarvieira.m3mangadex.core.model.ScanlationGroup

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class EndOfChapterScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    private fun chapter(
        number: String,
        title: String,
    ) = Chapter("c$number", "m", "1", number, title, "en", 40, null, null, listOf(ScanlationGroup("k", "Karma Scans", false)))

    @Test
    fun withNext() {
        compose.setContent {
            M3MangaDexTheme(darkTheme = true, dynamicColor = false) {
                Box(Modifier.background(Color.Black)) {
                    EndOfChapter(
                        chapter = chapter("1", "The Fukurogi Incident"),
                        neighbors = ChapterNeighbors(previous = null, next = chapter("2", "My Goal")),
                        onOpenChapter = {},
                        onBack = {},
                    )
                }
            }
        }
        compose.onRoot().captureRoboImage("src/test/screenshots/end_of_chapter.png")
    }
}
