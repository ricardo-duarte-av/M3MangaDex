package pt.aguiarvieira.m3mangadex.core.network.dto

import pt.aguiarvieira.m3mangadex.core.model.Chapter
import pt.aguiarvieira.m3mangadex.core.model.ContentRating
import pt.aguiarvieira.m3mangadex.core.model.Demographic
import pt.aguiarvieira.m3mangadex.core.model.Localized
import pt.aguiarvieira.m3mangadex.core.model.Manga
import pt.aguiarvieira.m3mangadex.core.model.MangaStats
import pt.aguiarvieira.m3mangadex.core.model.PublicationStatus
import pt.aguiarvieira.m3mangadex.core.model.ScanlationGroup
import pt.aguiarvieira.m3mangadex.core.model.Tag
import pt.aguiarvieira.m3mangadex.core.model.TagGroup
import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

internal fun MangaDto.toModel(): Manga =
    Manga(
        id = id,
        title = attributes.title,
        altTitles = attributes.altTitles,
        description = attributes.description,
        originalLanguage = attributes.originalLanguage,
        status = PublicationStatus.of(attributes.status),
        demographic = Demographic.of(attributes.publicationDemographic),
        contentRating = ContentRating.of(attributes.contentRating),
        year = attributes.year,
        tags = attributes.tags.map { it.toModel() },
        authors = relationships.names("author"),
        artists = relationships.names("artist"),
        coverFileName = relationships.firstOrNull { it.type == "cover_art" }?.string("fileName"),
        availableLanguages = attributes.availableTranslatedLanguages.filterNotNull(),
        lastVolume = attributes.lastVolume?.takeIf(String::isNotBlank),
        lastChapter = attributes.lastChapter?.takeIf(String::isNotBlank),
        updatedAt = attributes.updatedAt?.let(::parseInstant),
    )

internal fun TagDto.toModel(): Tag =
    Tag(
        id = id,
        name = Localized.pick(attributes.name, listOf("en")).orEmpty(),
        group = TagGroup.of(attributes.group),
    )

internal fun ChapterDto.toModel(): Chapter =
    Chapter(
        id = id,
        mangaId = relationships.firstOrNull { it.type == "manga" }?.id,
        volume = attributes.volume?.takeIf(String::isNotBlank),
        number = attributes.chapter?.takeIf(String::isNotBlank),
        title = attributes.title?.takeIf(String::isNotBlank),
        language = attributes.translatedLanguage,
        pages = attributes.pages,
        externalUrl = attributes.externalUrl?.takeIf(String::isNotBlank),
        readableAt = attributes.readableAt?.let(::parseInstant),
        groups =
            relationships
                .filter { it.type == "scanlation_group" }
                .map { ScanlationGroup(it.id, it.string("name") ?: "", it.boolean("official")) },
    )

internal fun MangaStatisticsDto.toModel() = MangaStats(follows = follows, rating = rating?.bayesian)

/** Author and artist names, de-duplicated (one person is often listed under both). */
private fun List<RelationshipDto>.names(type: String) =
    filter { it.type == type }.mapNotNull { it.string("name") }.distinct()

private fun parseInstant(value: String): Instant? =
    try {
        OffsetDateTime.parse(value).toInstant()
    } catch (_: DateTimeParseException) {
        null
    }
