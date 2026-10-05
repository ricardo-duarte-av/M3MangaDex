package pt.aguiarvieira.m3mangadex.core.network.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import pt.aguiarvieira.m3mangadex.core.network.AccessTokenProvider
import pt.aguiarvieira.m3mangadex.core.network.AtHomeReportInterceptor
import pt.aguiarvieira.m3mangadex.core.network.AuthInterceptor
import pt.aguiarvieira.m3mangadex.core.network.MangaDexApi
import pt.aguiarvieira.m3mangadex.core.network.MangaDexAuthApi
import pt.aguiarvieira.m3mangadex.core.network.MangaDexUserApi
import pt.aguiarvieira.m3mangadex.core.network.RateLimitInterceptor
import pt.aguiarvieira.m3mangadex.core.network.RateLimiter
import pt.aguiarvieira.m3mangadex.core.network.TokenAuthenticator
import pt.aguiarvieira.m3mangadex.core.network.UserAgentInterceptor
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * The User-Agent every request carries; the app provides it (it knows its version). A data class,
 * not a value class: Hilt can't generate providers for functions whose names Kotlin mangles.
 */
data class UserAgent(
    val value: String,
)

/** For api.mangadex.org: paced, and the only client that carries credentials (on [Authenticated] requests). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApiClient

/** For images (covers, chapter pages): never carries credentials, never touches the API's limit. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ImageClient

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    private const val API_HOST = "api.mangadex.org"

    // A little under MangaDex's ~5/s, leaving room for the at-home report calls (M2).
    private const val REQUESTS_PER_SECOND = 4
    private const val SECOND_NANOS = 1_000_000_000L
    private const val TIMEOUT_SECONDS = 30L

    @Provides
    @Singleton
    fun json(): Json =
        Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
            explicitNulls = false
        }

    @Provides
    @Singleton
    fun baseClient(userAgent: UserAgent): OkHttpClient =
        OkHttpClient
            .Builder()
            .addInterceptor(UserAgentInterceptor(userAgent.value))
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()

    @Provides
    @Singleton
    @ApiClient
    fun apiClient(
        base: OkHttpClient,
        tokens: AccessTokenProvider,
    ): OkHttpClient =
        base
            .newBuilder()
            .addInterceptor(
                RateLimitInterceptor(RateLimiter(REQUESTS_PER_SECOND, SECOND_NANOS), limits = { it.host == API_HOST }),
            )
            // After the limiter, so a retried request is paced too; the token goes on last.
            .addInterceptor(AuthInterceptor(tokens))
            .authenticator(TokenAuthenticator(tokens))
            .build()

    @Provides
    @Singleton
    @ImageClient
    fun imageClient(
        base: OkHttpClient,
        json: Json,
    ): OkHttpClient = base.newBuilder().addInterceptor(AtHomeReportInterceptor(reporter = base, json = json)).build()

    @Provides
    @Singleton
    fun mangaDexApi(
        @ApiClient client: OkHttpClient,
        json: Json,
    ): MangaDexApi = MangaDexApi(client, json)

    @Provides
    @Singleton
    fun mangaDexUserApi(
        @ApiClient client: OkHttpClient,
        json: Json,
    ): MangaDexUserApi = MangaDexUserApi(client, json)

    /** The auth server: the plain client, never the API one (no token, no API rate limit). */
    @Provides
    @Singleton
    fun mangaDexAuthApi(
        base: OkHttpClient,
        json: Json,
    ): MangaDexAuthApi = MangaDexAuthApi(base, json)
}
