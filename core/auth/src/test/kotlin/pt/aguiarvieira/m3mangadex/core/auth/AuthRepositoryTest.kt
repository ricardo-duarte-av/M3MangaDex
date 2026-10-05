package pt.aguiarvieira.m3mangadex.core.auth

import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pt.aguiarvieira.m3mangadex.core.network.AuthException
import pt.aguiarvieira.m3mangadex.core.network.MangaDexAuthApi
import pt.aguiarvieira.m3mangadex.core.network.di.NetworkModule

class AuthRepositoryTest {
    private val server = MockWebServer().apply { start() }
    private var now = 1_000_000L
    private val store = MemoryStore()
    private val api = MangaDexAuthApi(OkHttpClient(), NetworkModule.json(), server.url("/token"), nowMillis = { now })

    private fun repository() = AuthRepository(store, api).also { it.nowMillis = { now } }

    @After
    fun tearDown() = server.close()

    private fun tokens(n: Int) =
        MockResponse
            .Builder()
            .code(200)
            .body(
                """{"access_token":"access-$n","refresh_token":"refresh-$n","expires_in":900,"refresh_expires_in":7776000,"token_type":"Bearer"}""",
            ).build()

    private fun error(
        code: Int,
        error: String,
        description: String,
    ) = MockResponse
        .Builder()
        .code(code)
        .body("""{"error":"$error","error_description":"$description"}""")
        .build()

    @Test
    fun `login stores the session and posts the password grant`() =
        runTest {
            server.enqueue(tokens(1))
            val repository = repository()
            repository.login(" client ", "secret", "daedric7", "pa&ss")

            val form = server.takeRequest().body!!.utf8()
            assertTrue(form, form.contains("grant_type=password"))
            assertTrue(form, form.contains("password=pa%26ss"))
            assertTrue(form, form.contains("client_id=client"))
            assertEquals(Session.LoggedIn("daedric7"), repository.session.value)
            assertEquals("access-1", repository.accessToken())
            // The password itself is never stored.
            assertFalse(store.session.toString().contains("pa&ss"))
        }

    @Test
    fun `a stale token is refreshed before use`() =
        runTest {
            server.enqueue(tokens(1))
            server.enqueue(tokens(2))
            val repository = repository()
            repository.login("client", "secret", "user", "pw")
            server.takeRequest()

            now += 850_000 // 14 minutes later: inside the one-minute margin.
            assertEquals("access-2", repository.accessToken())
            assertTrue(
                server
                    .takeRequest()
                    .body!!
                    .utf8()
                    .contains("grant_type=refresh_token")
            )
        }

    @Test
    fun `a rejected token refreshes once, then reuses the new one`() =
        runTest {
            server.enqueue(tokens(1))
            server.enqueue(tokens(2))
            val repository = repository()
            repository.login("client", "secret", "user", "pw")

            assertEquals("access-2", repository.refreshAfterRejection("access-1"))
            // A second request that also saw access-1 rejected gets the fresh one without refreshing.
            assertEquals("access-2", repository.refreshAfterRejection("access-1"))
            assertEquals(2, server.requestCount)
        }

    @Test
    fun `a dead refresh token expires the session but keeps the client`() =
        runTest {
            server.enqueue(tokens(1))
            server.enqueue(error(400, "invalid_grant", "Token is not active"))
            val repository = repository()
            repository.login("client", "secret", "user", "pw")
            now += 1_000_000

            assertNull(repository.accessToken())
            assertEquals(Session.Expired("user"), repository.session.value)
            assertEquals(SavedClient("client", "secret", "user"), repository.savedClient())
            // And it stays expired across restarts.
            assertEquals(Session.Expired("user"), repository().session.value)
        }

    @Test
    fun `login failures say what went wrong`() =
        runTest {
            server.enqueue(error(401, "invalid_grant", "Invalid user credentials"))
            server.enqueue(error(401, "invalid_client", "Invalid client credentials"))
            val repository = repository()
            assertTrue(runCatching { repository.login("c", "s", "u", "p") }.exceptionOrNull() is AuthException.BadCredentials)
            assertTrue(runCatching { repository.login("c", "s", "u", "p") }.exceptionOrNull() is AuthException.BadClient)
            assertEquals(Session.LoggedOut, repository.session.value)
        }

    @Test
    fun `logout forgets the tokens but not the client`() =
        runTest {
            server.enqueue(tokens(1))
            val repository = repository()
            repository.login("client", "secret", "user", "pw")
            repository.logout()
            assertEquals(Session.LoggedOut, repository.session.value)
            assertNull(repository.accessToken())
            assertEquals("client", repository.savedClient()?.clientId)
        }

    private class MemoryStore : SessionStore {
        var session: StoredSession? = null

        override fun read() = session

        override fun write(session: StoredSession?) {
            this.session = session
        }
    }
}
