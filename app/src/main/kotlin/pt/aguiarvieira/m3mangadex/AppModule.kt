package pt.aguiarvieira.m3mangadex

import android.os.Build
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import pt.aguiarvieira.m3mangadex.core.network.di.UserAgent

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    /** MangaDex asks for an honest, identifying User-Agent. */
    @Provides
    fun userAgent(): UserAgent =
        UserAgent(
            "M3MangaDex/${BuildConfig.VERSION_NAME} (Android ${Build.VERSION.RELEASE}; " +
                "+https://github.com/ricardo-duarte-av/M3MangaDex)",
        )
}
