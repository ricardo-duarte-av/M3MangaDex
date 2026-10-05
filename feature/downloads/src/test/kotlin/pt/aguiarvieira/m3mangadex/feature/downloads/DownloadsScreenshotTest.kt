package pt.aguiarvieira.m3mangadex.feature.downloads

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import pt.aguiarvieira.m3mangadex.core.designsystem.theme.M3MangaDexTheme
import pt.aguiarvieira.m3mangadex.core.model.Download
import pt.aguiarvieira.m3mangadex.core.model.DownloadState
import java.time.Instant

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class DownloadsScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    private fun download(
        number: String,
        state: DownloadState,
        done: Int,
        size: Long,
    ) = Download("c$number", "m", "The Unwanted Undead Adventurer", null, number, "Chapter $number", "en", state, done, 24, size, Instant.EPOCH)

    @Test
    fun downloads() {
        val group =
            DownloadGroup(
                "m",
                "The Unwanted Undead Adventurer",
                null,
                listOf(
                    download("59", DownloadState.Done, 24, 6_100_000),
                    download("60", DownloadState.Downloading, 9, 0),
                    download("61", DownloadState.Queued, 0, 0),
                    download("62", DownloadState.Failed, 3, 0),
                ),
            )
        compose.mainClock.autoAdvance = false
        compose.setContent {
            M3MangaDexTheme(darkTheme = true, dynamicColor = false) {
                DownloadsScreen(listOf(group), onBack = {}, onRead = { _, _ -> }, onDelete = {}, onRetry = {}, onDeleteManga = {})
            }
        }
        compose.onRoot().captureRoboImage("src/test/screenshots/downloads.png")
    }
}
