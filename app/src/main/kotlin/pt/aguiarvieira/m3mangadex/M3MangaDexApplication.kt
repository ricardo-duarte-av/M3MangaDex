package pt.aguiarvieira.m3mangadex

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import dagger.hilt.android.HiltAndroidApp
import okhttp3.OkHttpClient
import pt.aguiarvieira.m3mangadex.core.network.di.ImageClient
import javax.inject.Inject

@HiltAndroidApp
class M3MangaDexApplication :
    Application(),
    SingletonImageLoader.Factory {
    @Inject
    @ImageClient
    lateinit var imageClient: OkHttpClient

    /** Every image loads through the image client: our User-Agent, never credentials. */
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader
            .Builder(context)
            .components { add(OkHttpNetworkFetcherFactory(callFactory = { imageClient })) }
            .crossfade(true)
            .build()
}
