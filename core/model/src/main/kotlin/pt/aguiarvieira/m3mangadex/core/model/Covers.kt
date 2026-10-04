package pt.aguiarvieira.m3mangadex.core.model

/** Cover image URLs on MangaDex's uploads server (no auth, cacheable). */
object Covers {
    enum class Size(
        val suffix: String,
    ) {
        /** 256px wide: lists and grids. */
        Thumbnail(".256.jpg"),

        /** 512px wide: details header, carousels. */
        Medium(".512.jpg"),
        Original(""),
    }

    fun url(
        mangaId: String,
        fileName: String,
        size: Size = Size.Thumbnail,
    ): String = "https://uploads.mangadex.org/covers/$mangaId/$fileName${size.suffix}"
}

fun Manga.coverUrl(size: Covers.Size = Covers.Size.Thumbnail): String? = coverFileName?.let { Covers.url(id, it, size) }
