package pt.aguiarvieira.m3mangadex

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import pt.aguiarvieira.m3mangadex.core.data.notify.AppNotifications
import pt.aguiarvieira.m3mangadex.core.data.notify.NewChaptersWorker
import pt.aguiarvieira.m3mangadex.core.datastore.PreferencesDataSource
import pt.aguiarvieira.m3mangadex.core.network.di.ImageClient
import javax.inject.Inject

@HiltAndroidApp
class M3MangaDexApplication :
    Application(),
    SingletonImageLoader.Factory {
    @Inject
    @ImageClient
    lateinit var imageClient: OkHttpClient

    @Inject lateinit var preferences: PreferencesDataSource

    private val scope = MainScope()

    override fun onCreate() {
        super.onCreate()
        AppNotifications.ensureChannels(this)
        // The periodic new-chapter check follows the setting.
        scope.launch {
            preferences.preferences.map { it.newChapterNotifications }.distinctUntilChanged().collect {
                NewChaptersWorker.schedule(this@M3MangaDexApplication, it)
            }
        }
    }

    /** Every image loads through the image client: our User-Agent, never credentials. */
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader
            .Builder(context)
            .components { add(OkHttpNetworkFetcherFactory(callFactory = { imageClient })) }
            .crossfade(true)
            .build()
}
