package pt.aguiarvieira.m3mangadex.feature.settings

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import pt.aguiarvieira.m3mangadex.core.designsystem.theme.M3MangaDexTheme
import pt.aguiarvieira.m3mangadex.core.model.ContentRating
import pt.aguiarvieira.m3mangadex.core.model.UserPreferences

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class SettingsScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun settings() {
        val preferences = UserPreferences(listOf("pt", "en"), ContentRating.Default, dataSaver = true)
        compose.setContent {
            M3MangaDexTheme(darkTheme = false, dynamicColor = false) {
                SettingsScreen(
                    preferences,
                    versionName = "0.1.0",
                    onToggleLanguage = {},
                    onToggleRating = {},
                    onDataSaverChange = {},
                    onSpreadsChange = {},
                    onVolumeKeysChange = {},
                    onCoverThemingChange = {},
                    onCropBordersChange = {},
                )
            }
        }
        compose.onRoot().captureRoboImage("src/test/screenshots/settings.png")
    }
}
