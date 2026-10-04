package pt.aguiarvieira.m3mangadex.core.designsystem

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import pt.aguiarvieira.m3mangadex.core.designsystem.component.ThemeCatalog
import pt.aguiarvieira.m3mangadex.core.designsystem.theme.M3MangaDexTheme

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class ThemeCatalogScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun light() = capture(dark = false, name = "theme_catalog_light")

    @Test
    fun dark() = capture(dark = true, name = "theme_catalog_dark")

    private fun capture(
        dark: Boolean,
        name: String,
    ) {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            M3MangaDexTheme(darkTheme = dark, dynamicColor = false) { ThemeCatalog() }
        }
        compose.onRoot().captureRoboImage("src/test/screenshots/$name.png")
    }
}
