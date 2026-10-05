package pt.aguiarvieira.m3mangadex.core.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import pt.aguiarvieira.m3mangadex.core.network.dto.ErrorResponseDto
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Marks a request that needs the user's token. Only these get an `Authorization` header (MangaDex
 * asks clients not to send credentials where they aren't needed: it defeats caching).
 */
object Authenticated

internal fun Request.Builder.authenticated(): Request.Builder = tag(Authenticated::class.java, Authenticated)

internal val Request.isAuthenticated: Boolean get() = tag(Authenticated::class.java) != null

/** Runs MangaDex requests: suspends on OkHttp, decodes JSON, turns error bodies into [MangaDexException]. */
internal class ApiCaller(
    private val client: OkHttpClient,
    private val json: Json,
) {
    suspend fun <T> send(
        request: Request,
        deserializer: DeserializationStrategy<T>,
    ): T {
        val body = sendForBody(request)
        return try {
            json.decodeFromString(deserializer, body)
        } catch (e: SerializationException) {
            throw IOException("Unexpected response from ${request.url.encodedPath}", e)
        }
    }

    /** For calls whose answer is just "ok". */
    suspend fun sendForBody(request: Request): String {
        val response = client.newCall(request).await()
        return withContext(Dispatchers.IO) {
            response.use {
                val body = it.body.string()
                if (!it.isSuccessful) throw MangaDexException(it.code, errorMessage(it.code, body))
                body
            }
        }
    }

    private fun errorMessage(
        code: Int,
        body: String,
    ): String {
        val error =
            runCatching { json.decodeFromString(ErrorResponseDto.serializer(), body) }
                .getOrNull()
                ?.errors
                ?.firstOrNull()
        return error?.detail ?: error?.title ?: "HTTP $code"
    }
}

private suspend fun Call.await(): Response =
    suspendCancellableCoroutine { continuation ->
        continuation.invokeOnCancellation { cancel() }
        enqueue(
            object : Callback {
                override fun onFailure(
                    call: Call,
                    e: IOException,
                ) = continuation.resumeWithException(e)

                override fun onResponse(
                    call: Call,
                    response: Response,
                ) = continuation.resume(response) { _, value, _ -> value.close() }
            },
        )
    }
