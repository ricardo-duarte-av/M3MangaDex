package pt.aguiarvieira.m3mangadex.core.network

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import pt.aguiarvieira.m3mangadex.core.model.AtHomeServer
import pt.aguiarvieira.m3mangadex.core.model.Chapter
import pt.aguiarvieira.m3mangadex.core.model.ContentRating
import pt.aguiarvieira.m3mangadex.core.model.Manga
import pt.aguiarvieira.m3mangadex.core.model.MangaFilter
import pt.aguiarvieira.m3mangadex.core.model.MangaOrder
import pt.aguiarvieira.m3mangadex.core.model.MangaStats
import pt.aguiarvieira.m3mangadex.core.model.Page
import pt.aguiarvieira.m3mangadex.core.model.Tag
import pt.aguiarvieira.m3mangadex.core.network.dto.AtHomeDto
import pt.aguiarvieira.m3mangadex.core.network.dto.ChapterDto
import pt.aguiarvieira.m3mangadex.core.network.dto.CollectionDto
import pt.aguiarvieira.m3mangadex.core.network.dto.EntityDto
import pt.aguiarvieira.m3mangadex.core.network.dto.MangaDto
import pt.aguiarvieira.m3mangadex.core.network.dto.StatisticsDto
import pt.aguiarvieira.m3mangadex.core.network.dto.TagDto
import pt.aguiarvieira.m3mangadex.core.network.dto.toModel

/**
 * The public (unauthenticated) MangaDex API. Requests go through [client], which paces them and
 * sets the User-Agent; nothing here ever sends credentials.
 */
class MangaDexApi(
    client: OkHttpClient,
    json: Json,
    private val baseUrl: HttpUrl = BASE_URL,
) {
    private val caller = ApiCaller(client, json)

    suspend fun searchManga(
        filter: MangaFilter,
        offset: Int,
        limit: Int,
    ): Page<Manga> {
        val url =
            url("manga") {
                addQueryParameter("limit", limit.toString())
                addQueryParameter("offset", offset.toString())
                if (filter.title.isNotBlank()) addQueryParameter("title", filter.title.trim())
                array("includes", listOf("cover_art"))
                array("includedTags", filter.includedTags)
                array("excludedTags", filter.excludedTags)
                array("status", filter.status.map { it.apiValue })
                array("publicationDemographic", filter.demographic.map { it.apiValue })
                array("contentRating", filter.contentRating.map { it.apiValue })
                array("originalLanguage", filter.originalLanguage)
                array("availableTranslatedLanguage", filter.availableLanguages)
                filter.createdAtSince?.let { addQueryParameter("createdAtSince", it) }
                // Relevance only means something with a title; without one MangaDex ignores it.
                val order =
                    if (filter.order == MangaOrder.Relevance &&
                        filter.title.isBlank()
                    ) {
                        MangaOrder.Follows
                    } else {
                        filter.order
                    }
                addQueryParameter("order[${order.field}]", if (order.ascending) "asc" else "desc")
            }
        val page = get(url, CollectionDto.serializer(MangaDto.serializer()))
        return Page(page.data.map { it.toModel() }, page.offset, page.total)
    }

    suspend fun manga(id: String): Manga {
        val url = url("manga/$id") { array("includes", listOf("cover_art", "author", "artist")) }
        return get(url, EntityDto.serializer(MangaDto.serializer())).data.toModel()
    }

    /** A manga's chapters in [languages], newest first. */
    suspend fun chapters(
        mangaId: String,
        languages: Collection<String>,
        contentRating: Collection<ContentRating>,
        offset: Int,
        limit: Int,
    ): Page<Chapter> {
        val url =
            url("manga/$mangaId/feed") {
                addQueryParameter("limit", limit.toString())
                addQueryParameter("offset", offset.toString())
                array("translatedLanguage", languages)
                // The feed filters chapters by their manga's rating; ask for all of them, since we
                // already chose to show this manga.
                array("contentRating", contentRating.map { it.apiValue })
                array("includes", listOf("scanlation_group"))
                addQueryParameter("order[volume]", "desc")
                addQueryParameter("order[chapter]", "desc")
            }
        val page = get(url, CollectionDto.serializer(ChapterDto.serializer()))
        return Page(page.data.map { it.toModel() }, page.offset, page.total)
    }

    suspend fun tags(): List<Tag> =
        get(
            url("manga/tag") {
            },
            CollectionDto.serializer(TagDto.serializer())
        ).data.map { it.toModel() }

    /** Several manga by id (library, feed covers), in pages of at most [IDS_PER_REQUEST]. */
    suspend fun mangaByIds(ids: Collection<String>): List<Manga> =
        ids.distinct().chunked(IDS_PER_REQUEST).flatMap { chunk ->
            val url =
                url("manga") {
                    addQueryParameter("limit", chunk.size.toString())
                    array("ids", chunk)
                    array("includes", listOf("cover_art"))
                    // The user already chose these; show them whatever their rating.
                    array("contentRating", ContentRating.entries.map { it.apiValue })
                }
            get(url, CollectionDto.serializer(MangaDto.serializer())).data.map { it.toModel() }
        }

    suspend fun chapter(id: String): Chapter {
        val url = url("chapter/$id") { array("includes", listOf("scanlation_group")) }
        return get(url, EntityDto.serializer(ChapterDto.serializer())).data.toModel()
    }

    /** Where to fetch [chapterId]'s pages from, for the next ~15 minutes. */
    suspend fun atHomeServer(chapterId: String): AtHomeServer {
        val dto = get(url("at-home/server/$chapterId") {}, AtHomeDto.serializer())
        return AtHomeServer(dto.baseUrl, dto.chapter.hash, dto.chapter.data, dto.chapter.dataSaver)
    }

    suspend fun statistics(mangaIds: Collection<String>): Map<String, MangaStats> {
        val url = url("statistics/manga") { array("manga", mangaIds) }
        return get(url, StatisticsDto.serializer()).statistics.mapValues { it.value.toModel() }
    }

    private inline fun url(
        path: String,
        build: HttpUrl.Builder.() -> Unit,
    ): HttpUrl =
        baseUrl
            .newBuilder()
            .addPathSegments(path)
            .apply(build)
            .build()

    private fun HttpUrl.Builder.array(
        name: String,
        values: Collection<String>,
    ) = values.forEach { addQueryParameter("$name[]", it) }

    private suspend fun <T> get(
        url: HttpUrl,
        deserializer: DeserializationStrategy<T>,
    ): T = caller.send(Request.Builder().url(url).build(), deserializer)

    companion object {
        val BASE_URL = "https://api.mangadex.org/".toHttpUrl()

        /** The API's paging cap: offset + limit may not exceed it. */
        const val MAX_RESULTS = 10_000

        /** MangaDex's `limit` ceiling for collections. */
        const val IDS_PER_REQUEST = 100
    }
}
