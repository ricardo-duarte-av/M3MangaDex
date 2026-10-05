package pt.aguiarvieira.m3mangadex.core.auth

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import pt.aguiarvieira.m3mangadex.core.network.AccessTokenProvider
import pt.aguiarvieira.m3mangadex.core.network.AuthException
import pt.aguiarvieira.m3mangadex.core.network.MangaDexAuthApi
import pt.aguiarvieira.m3mangadex.core.network.Tokens
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The MangaDex session. Logs in with a personal API client (the only kind MangaDex offers third-
 * party apps today) and keeps the 15-minute access token fresh for the API client.
 *
 * Refreshes are serialised: when several requests find the token stale at once, one refreshes and
 * the rest use its result. A refresh token MangaDex no longer accepts moves the session to
 * [Session.Expired], so the user is asked for the password again rather than silently logged out.
 */
@Singleton
class AuthRepository
    @Inject
    internal constructor(
        private val store: SessionStore,
        private val api: MangaDexAuthApi,
    ) : AccessTokenProvider {
        private val lock = Any()
        private var stored: StoredSession? = store.read()
        private val _session = MutableStateFlow(sessionOf(stored))
        val session: StateFlow<Session> = _session.asStateFlow()

        /** Test hook and seam: "now" in epoch millis. */
        internal var nowMillis: () -> Long = System::currentTimeMillis

        /** The client and user to pre-fill the login form with. */
        fun savedClient(): SavedClient? =
            synchronized(lock) {
                stored?.let { SavedClient(it.clientId, it.clientSecret, it.username) }
            }

        /** Logs in; throws [AuthException] (or [IOException] when offline) on failure. */
        suspend fun login(
            clientId: String,
            clientSecret: String,
            username: String,
            password: String,
        ) {
            val tokens =
                withContext(
                    Dispatchers.IO
                ) { api.login(clientId.trim(), clientSecret.trim(), username.trim(), password) }
            synchronized(lock) {
                save(StoredSession(clientId.trim(), clientSecret.trim(), username.trim()).with(tokens))
            }
        }

        /** Forgets the tokens; the client is kept so logging back in is quicker. */
        fun logout(): Unit =
            synchronized(lock) {
                val current = stored ?: return
                save(
                    current.copy(
                        accessToken = null,
                        refreshToken = null,
                        accessExpiresAt = 0,
                        refreshExpiresAt = 0,
                        expired = false
                    )
                )
            }

        override fun accessToken(): String? =
            synchronized(lock) {
                val current = stored ?: return null
                val access = current.accessToken ?: return null
                if (current.accessExpiresAt - nowMillis() > EXPIRY_MARGIN_MILLIS) access else refreshLocked()
            }

        override fun refreshAfterRejection(rejected: String): String? =
            synchronized(lock) {
                val access = stored?.accessToken ?: return null
                // Another request already refreshed while this one was in flight.
                if (access != rejected) access else refreshLocked()
            }

        private fun refreshLocked(): String? {
            val current = stored ?: return null
            val refresh = current.refreshToken ?: return null
            return try {
                val tokens = api.refresh(current.clientId, current.clientSecret, refresh)
                save(current.with(tokens))
                tokens.accessToken
            } catch (_: AuthException) {
                save(current.copy(accessToken = null, refreshToken = null, expired = true))
                null
            } catch (_: IOException) {
                // Offline: keep the session; the request fails and the next one tries again.
                null
            }
        }

        private fun save(session: StoredSession) {
            stored = session
            store.write(session)
            _session.value = sessionOf(session)
        }

        private fun StoredSession.with(tokens: Tokens) =
            copy(
                expired = false,
                accessToken = tokens.accessToken,
                refreshToken = tokens.refreshToken,
                accessExpiresAt = tokens.accessExpiresAt,
                refreshExpiresAt = tokens.refreshExpiresAt,
            )

        private fun sessionOf(session: StoredSession?): Session =
            when {
                session == null -> Session.LoggedOut
                session.refreshToken != null -> Session.LoggedIn(session.username)
                session.expired -> Session.Expired(session.username)
                else -> Session.LoggedOut
            }

        private companion object {
            /** Refresh a little early, so a token doesn't expire mid-request. */
            const val EXPIRY_MARGIN_MILLIS = 60_000L
        }
    }
