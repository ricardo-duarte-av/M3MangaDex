package pt.aguiarvieira.m3mangadex.core.auth

/** Whether the user is logged in to MangaDex. */
sealed interface Session {
    data object LoggedOut : Session

    data class LoggedIn(
        val username: String,
    ) : Session

    /**
     * The refresh token ran out (90 days unused) or was revoked. The client and username are kept,
     * so logging back in only needs the password.
     */
    data class Expired(
        val username: String,
    ) : Session
}

/** What the login form can be pre-filled with: the last client and user (never the password). */
data class SavedClient(
    val clientId: String,
    val clientSecret: String,
    val username: String,
)
