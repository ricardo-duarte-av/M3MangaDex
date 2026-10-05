package pt.aguiarvieira.m3mangadex.core.auth

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import pt.aguiarvieira.m3mangadex.core.network.AccessTokenProvider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface AuthModule {
    @Binds
    fun accessTokens(repository: AuthRepository): AccessTokenProvider

    companion object {
        @Provides
        @Singleton
        fun sessionStore(
            @ApplicationContext context: Context,
            json: Json,
        ): SessionStore = KeystoreSessionStore(context, json)
    }
}
