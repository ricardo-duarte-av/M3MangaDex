package pt.aguiarvieira.m3mangadex.core.network

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import pt.aguiarvieira.m3mangadex.core.network.dto.TokenDto
import java.io.IOException

/** Fresh tokens from MangaDex's auth server. */
data class Tokens(
    val accessToken: String,
    val refreshToken: String,
    /** Epoch millis. */
    val accessExpiresAt: Long,
    val refreshExpiresAt: Long,
)

/** Why the auth server said no. */
sealed class AuthException(
    message: String,
) : IOException(message) {
    /** Wrong username or password, or an account that isn't verified yet. */
    class BadCredentials(
        message: String,
    ) : AuthException(message)

    /** The client id or secret is wrong, or the client isn't approved (yet). */
    class BadClient(
        message: String,
    ) : AuthException(message)

    /** The refresh token expired or was revoked: the password is needed again. */
    class SessionExpired(
        message: String,
    ) : AuthException(message)

    class TooManyAttempts(
        message: String,
    ) : AuthException(message)
}

/**
 * MangaDex's OpenID Connect token endpoint, for personal API clients: the `password` grant to log
 * in, `refresh_token` to renew the 15-minute access token. Blocking: refreshes happen inside
 * OkHttp's interceptor chain, so callers pick the thread.
 */
class MangaDexAuthApi(
    private val client: OkHttpClient,
    private val json: Json,
    private val tokenUrl: HttpUrl = TOKEN_URL,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {
    fun login(
        clientId: String,
        clientSecret: String,
        username: String,
        password: String,
    ): Tokens =
        request(
            FormBody
                .Builder()
                .add("grant_type", "password")
                .add("username", username)
                .add("password", password)
                .add("client_id", clientId)
                .add("client_secret", clientSecret)
                .build(),
            refreshing = false,
        )

    fun refresh(
        clientId: String,
        clientSecret: String,
        refreshToken: String,
    ): Tokens =
        request(
            FormBody
                .Builder()
                .add("grant_type", "refresh_token")
                .add("refresh_token", refreshToken)
                .add("client_id", clientId)
                .add("client_secret", clientSecret)
                .build(),
            refreshing = true,
        )

    @Suppress("ThrowsCount") // One per way the auth server can say no; each maps to a different message.
    private fun request(
        form: FormBody,
        refreshing: Boolean,
    ): Tokens {
        val start = nowMillis()
        client
            .newCall(
                Request
                    .Builder()
                    .url(tokenUrl)
                    .post(form)
                    .build()
            ).execute()
            .use { response ->
                if (response.code == TOO_MANY_REQUESTS) {
                    throw AuthException.TooManyAttempts("Too many login attempts; try again later")
                }
                val dto =
                    try {
                        json.decodeFromString(TokenDto.serializer(), response.body.string())
                    } catch (e: SerializationException) {
                        throw IOException("Unexpected answer from the login server (HTTP ${response.code})", e)
                    }
                val access = dto.accessToken
                val refresh = dto.refreshToken
                if (response.isSuccessful && access != null && refresh != null) {
                    return Tokens(
                        accessToken = access,
                        refreshToken = refresh,
                        accessExpiresAt = start + dto.expiresIn * MILLIS,
                        refreshExpiresAt = start + dto.refreshExpiresIn * MILLIS,
                    )
                }
                throw failure(dto, refreshing)
            }
    }

    /** Keycloak's error codes, mapped to what the user can do about them. */
    private fun failure(
        dto: TokenDto,
        refreshing: Boolean,
    ): AuthException {
        val description = dto.errorDescription ?: dto.error ?: "Login failed"
        return when {
            dto.error in CLIENT_ERRORS -> AuthException.BadClient(description)
            refreshing && dto.error == "invalid_grant" -> AuthException.SessionExpired(description)
            else -> AuthException.BadCredentials(description)
        }
    }

    companion object {
        val TOKEN_URL = "https://auth.mangadex.org/realms/mangadex/protocol/openid-connect/token".toHttpUrl()
        private const val TOO_MANY_REQUESTS = 429
        private val CLIENT_ERRORS = setOf("invalid_client", "unauthorized_client")
        private const val MILLIS = 1_000L
    }
}
