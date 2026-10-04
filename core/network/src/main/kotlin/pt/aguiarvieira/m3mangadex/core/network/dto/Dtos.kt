package pt.aguiarvieira.m3mangadex.core.network.dto

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

// Wire types for the slice of the MangaDex API the app uses. Everything defaults, so a field
// MangaDex drops or nulls degrades a screen instead of failing the whole response.

@Serializable
internal data class CollectionDto<T>(
    val data: List<T> = emptyList(),
    val limit: Int = 0,
    val offset: Int = 0,
    val total: Int = 0,
)

@Serializable
internal data class EntityDto<T>(
    val data: T,
)

@Serializable
internal data class MangaDto(
    val id: String,
    val attributes: MangaAttributesDto = MangaAttributesDto(),
    val relationships: List<RelationshipDto> = emptyList(),
)

@Serializable
internal data class MangaAttributesDto(
    @Serializable(LocalizedTextSerializer::class) val title: Map<String, String> = emptyMap(),
    val altTitles: List<
        @Serializable(LocalizedTextSerializer::class)
        Map<String, String>
    > = emptyList(),
    @Serializable(LocalizedTextSerializer::class) val description: Map<String, String> = emptyMap(),
    val originalLanguage: String = "",
    val status: String? = null,
    val publicationDemographic: String? = null,
    val contentRating: String? = null,
    val year: Int? = null,
    val tags: List<TagDto> = emptyList(),
    val availableTranslatedLanguages: List<String?> = emptyList(),
    val lastVolume: String? = null,
    val lastChapter: String? = null,
    val updatedAt: String? = null,
)

@Serializable
internal data class TagDto(
    val id: String,
    val attributes: TagAttributesDto = TagAttributesDto(),
)

@Serializable
internal data class TagAttributesDto(
    @Serializable(LocalizedTextSerializer::class) val name: Map<String, String> = emptyMap(),
    val group: String? = null,
)

/**
 * A JSON:API relationship. With `includes[]=…` the related entity's [attributes] are inlined;
 * otherwise only [id] and [type] are present. [related] is set on manga↔manga links (sequel, …).
 */
@Serializable
internal data class RelationshipDto(
    val id: String,
    val type: String,
    val related: String? = null,
    val attributes: JsonObject? = null,
) {
    fun string(key: String): String? = (attributes?.get(key) as? JsonPrimitive)?.contentOrNull

    fun boolean(key: String): Boolean = string(key).toBoolean()
}

@Serializable
internal data class ChapterDto(
    val id: String,
    val attributes: ChapterAttributesDto = ChapterAttributesDto(),
    val relationships: List<RelationshipDto> = emptyList(),
)

@Serializable
internal data class ChapterAttributesDto(
    val volume: String? = null,
    val chapter: String? = null,
    val title: String? = null,
    val translatedLanguage: String = "",
    val externalUrl: String? = null,
    val pages: Int = 0,
    val readableAt: String? = null,
)

@Serializable
internal data class StatisticsDto(
    val statistics: Map<String, MangaStatisticsDto> = emptyMap(),
)

@Serializable
internal data class MangaStatisticsDto(
    val follows: Int? = null,
    val rating: RatingDto? = null,
)

@Serializable
internal data class RatingDto(
    val average: Double? = null,
    val bayesian: Double? = null,
)

@Serializable
internal data class ErrorResponseDto(
    val errors: List<ErrorDto> = emptyList(),
)

@Serializable
internal data class ErrorDto(
    val status: Int = 0,
    val title: String? = null,
    val detail: String? = null,
)

/**
 * MangaDex sends an empty localized map as `[]` instead of `{}` (PHP's empty array), and the odd
 * non-string value; both decode to whatever strings there are.
 */
internal object LocalizedTextSerializer : KSerializer<Map<String, String>> {
    private val delegate = JsonElement.serializer()
    override val descriptor: SerialDescriptor = delegate.descriptor

    override fun deserialize(decoder: Decoder): Map<String, String> {
        val element = (decoder as JsonDecoder).decodeJsonElement()
        return (element as? JsonObject)
            ?.mapNotNull { (key, value) -> (value as? JsonPrimitive)?.contentOrNull?.let { key to it } }
            ?.toMap()
            .orEmpty()
    }

    override fun serialize(
        encoder: Encoder,
        value: Map<String, String>,
    ): Unit = throw UnsupportedOperationException("decode only")
}
