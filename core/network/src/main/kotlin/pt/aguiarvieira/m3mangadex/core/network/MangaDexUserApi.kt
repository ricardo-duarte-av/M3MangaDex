package pt.aguiarvieira.m3mangadex.core.network

import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import pt.aguiarvieira.m3mangadex.core.model.ContentRating
import pt.aguiarvieira.m3mangadex.core.model.FeedEntry
import pt.aguiarvieira.m3mangadex.core.model.Page
import pt.aguiarvieira.m3mangadex.core.model.ReadingStatus
import pt.aguiarvieira.m3mangadex.core.network.dto.ChapterDto
import pt.aguiarvieira.m3mangadex.core.network.dto.CollectionDto
import pt.aguiarvieira.m3mangadex.core.network.dto.EntityDto
import pt.aguiarvieira.m3mangadex.core.network.dto.ReadMarkersDto
import pt.aguiarvieira.m3mangadex.core.network.dto.ReadMarkersUpdateDto
import pt.aguiarvieira.m3mangadex.core.network.dto.StatusDto
import pt.aguiarvieira.m3mangadex.core.network.dto.StatusesDto
import pt.aguiarvieira.m3mangadex.core.network.dto.UserDto
import pt.aguiarvieira.m3mangadex.core.network.dto.includedManga
import pt.aguiarvieira.m3mangadex.core.network.dto.toModel

/**
 * The logged-in user's side of MangaDex: library statuses, follows, read markers and the follows
 * feed. Every request here is [Authenticated]; without a session they fail with 401.
 */
class MangaDexUserApi(
    client: OkHttpClient,
    private val json: Json,
    private val baseUrl: HttpUrl = MangaDexApi.BASE_URL,
) {
    private val caller = ApiCaller(client, json)

    suspend fun username(): String =
        caller
            .send(get("user/me"), EntityDto.serializer(UserDto.serializer()))
            .data.attributes.username

    /** Every manga in the user's library, with its status. */
    suspend fun statuses(): Map<String, ReadingStatus> =
        caller
            .send(get("manga/status"), StatusesDto.serializer())
            .statuses
            .mapNotNull { (id, status) -> ReadingStatus.of(status)?.let { id to it } }
            .toMap()

    suspend fun status(mangaId: String): ReadingStatus? =
        ReadingStatus.of(caller.send(get("manga/$mangaId/status"), StatusDto.serializer()).status)

    /** Puts [mangaId] in the library under [status], or takes it out (null). */
    suspend fun setStatus(
        mangaId: String,
        status: ReadingStatus?,
    ) {
        val body = json.encodeToString(StatusDto.serializer(), StatusDto(status?.apiValue)).toRequestBody(JSON)
        caller.sendForBody(request("manga/$mangaId/status").post(body).build())
    }

    suspend fun isFollowing(mangaId: String): Boolean =
        try {
            caller.sendForBody(get("user/follows/manga/$mangaId"))
            true
        } catch (e: MangaDexException) {
            if (e.status == NOT_FOUND) false else throw e
        }

    suspend fun setFollowing(
        mangaId: String,
        following: Boolean,
    ) {
        val builder = request("manga/$mangaId/follow")
        caller.sendForBody((if (following) builder.post(EMPTY) else builder.delete()).build())
    }

    /** Ids of [mangaId]'s chapters the user has read on MangaDex. */
    suspend fun readChapters(mangaId: String): Set<String> =
        caller.send(get("manga/$mangaId/read"), ReadMarkersDto.serializer()).data.toSet()

    /**
     * Marks chapters read or unread. (Not `updateHistory=true`: MangaDex currently rejects it with
     * "Persistent history is temporarily disabled", failing the whole request.)
     */
    suspend fun markChapters(
        mangaId: String,
        read: List<String>,
        unread: List<String>,
    ) {
        val body =
            json
                .encodeToString(
                    ReadMarkersUpdateDto.serializer(),
                    ReadMarkersUpdateDto(read, unread)
                ).toRequestBody(JSON)
        caller.sendForBody(request("manga/$mangaId/read").post(body).build())
    }

    /** New chapters of followed manga, newest first. */
    suspend fun followsFeed(
        languages: Collection<String>,
        contentRating: Collection<ContentRating>,
        offset: Int,
        limit: Int,
    ): Page<FeedEntry> {
        val url =
            url("user/follows/manga/feed")
                .newBuilder()
                .addQueryParameter("limit", limit.toString())
                .addQueryParameter("offset", offset.toString())
                .apply {
                    languages.forEach { addQueryParameter("translatedLanguage[]", it) }
                    contentRating.forEach { addQueryParameter("contentRating[]", it.apiValue) }
                    listOf("manga", "scanlation_group").forEach { addQueryParameter("includes[]", it) }
                }.addQueryParameter("order[readableAt]", "desc")
                .build()
        val page =
            caller.send(
                Request
                    .Builder()
                    .url(url)
                    .authenticated()
                    .build(),
                CollectionDto.serializer(ChapterDto.serializer())
            )
        return Page(page.data.map { FeedEntry(it.toModel(), it.includedManga(json)) }, page.offset, page.total)
    }

    private fun url(path: String): HttpUrl = baseUrl.newBuilder().addPathSegments(path).build()

    private fun request(path: String): Request.Builder = Request.Builder().url(url(path)).authenticated()

    private fun get(path: String): Request = request(path).build()

    private companion object {
        const val NOT_FOUND = 404
        val JSON = "application/json".toMediaType()
        val EMPTY = ByteArray(0).toRequestBody(null)
    }
}
