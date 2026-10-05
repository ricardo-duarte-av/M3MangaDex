package pt.aguiarvieira.m3mangadex.core.network

import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

/**
 * Supplies the user's access token to the API client. Implemented by the session (core:auth);
 * both calls block, as they run on OkHttp's threads.
 */
interface AccessTokenProvider {
    /** A token valid for a little while yet (refreshed first if it's about to expire), or null when logged out. */
    fun accessToken(): String?

    /** MangaDex rejected [rejected]; refresh (unless that already happened) and return the new token, or null. */
    fun refreshAfterRejection(rejected: String): String?
}

/** Adds `Authorization: Bearer …` to [Authenticated] requests, and only to those. */
class AuthInterceptor(
    private val tokens: AccessTokenProvider,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (!request.isAuthenticated) return chain.proceed(request)
        val token = tokens.accessToken() ?: return chain.proceed(request)
        return chain.proceed(request.withToken(token))
    }
}

/** On a 401 to an [Authenticated] request, refreshes the token once and retries. */
class TokenAuthenticator(
    private val tokens: AccessTokenProvider,
) : Authenticator {
    override fun authenticate(
        route: Route?,
        response: Response,
    ): Request? {
        val request = response.request
        if (!request.isAuthenticated || response.priorResponse != null) return null
        val rejected = request.header(AUTHORIZATION)?.removePrefix(BEARER) ?: return null
        val token = tokens.refreshAfterRejection(rejected) ?: return null
        return request.withToken(token)
    }
}

private fun Request.withToken(token: String) = newBuilder().header(AUTHORIZATION, BEARER + token).build()

private const val AUTHORIZATION = "Authorization"
private const val BEARER = "Bearer "
