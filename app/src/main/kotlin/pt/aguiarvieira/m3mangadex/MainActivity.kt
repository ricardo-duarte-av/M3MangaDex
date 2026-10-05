package pt.aguiarvieira.m3mangadex

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.map
import pt.aguiarvieira.m3mangadex.core.datastore.PreferencesDataSource
import pt.aguiarvieira.m3mangadex.core.designsystem.theme.LocalCoverTheming
import pt.aguiarvieira.m3mangadex.core.designsystem.theme.M3MangaDexTheme
import pt.aguiarvieira.m3mangadex.navigation.M3MangaDexApp
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var preferences: PreferencesDataSource

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        WindowCompat.enableEdgeToEdge(window)
        super.onCreate(savedInstanceState)
        val coverTheming = preferences.preferences.map { it.coverTheming }
        setContent {
            val themed by coverTheming.collectAsStateWithLifecycle(initialValue = true)
            M3MangaDexTheme {
                CompositionLocalProvider(LocalCoverTheming provides themed) {
                    M3MangaDexApp()
                }
            }
        }
    }
}
