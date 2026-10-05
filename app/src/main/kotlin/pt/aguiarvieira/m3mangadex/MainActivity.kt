package pt.aguiarvieira.m3mangadex

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import pt.aguiarvieira.m3mangadex.core.data.notify.AppNotifications
import pt.aguiarvieira.m3mangadex.core.datastore.PreferencesDataSource
import pt.aguiarvieira.m3mangadex.core.designsystem.theme.LocalCoverTheming
import pt.aguiarvieira.m3mangadex.core.designsystem.theme.M3MangaDexTheme
import pt.aguiarvieira.m3mangadex.navigation.M3MangaDexApp
import pt.aguiarvieira.m3mangadex.navigation.OpenRequest
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var preferences: PreferencesDataSource

    /** A notification's manga or chapter, until navigation has shown it. */
    private val open = MutableStateFlow<OpenRequest?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        WindowCompat.enableEdgeToEdge(window)
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) take(intent)
        val coverTheming = preferences.preferences.map { it.coverTheming }
        setContent {
            val themed by coverTheming.collectAsStateWithLifecycle(initialValue = true)
            val request by open.collectAsStateWithLifecycle()
            M3MangaDexTheme {
                CompositionLocalProvider(LocalCoverTheming provides themed) {
                    M3MangaDexApp(open = request, onOpenHandle = { open.value = null })
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        take(intent)
    }

    private fun take(intent: Intent?) {
        val mangaId = intent?.getStringExtra(AppNotifications.EXTRA_MANGA_ID) ?: return
        open.value = OpenRequest(mangaId, intent.getStringExtra(AppNotifications.EXTRA_CHAPTER_ID))
    }
}
