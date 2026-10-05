package pt.aguiarvieira.m3mangadex.core.model

/**
 * Where a chapter's pages are served from right now: a MangaDex@Home node (or MangaDex itself),
 * valid for about 15 minutes. Page URLs are `{baseUrl}/{data|data-saver}/{hash}/{file}`.
 */
data class AtHomeServer(
    val baseUrl: String,
    val hash: String,
    val data: List<String>,
    val dataSaver: List<String>,
) {
    val pageCount: Int get() = data.size

    fun pageUrl(
        index: Int,
        dataSaver: Boolean,
    ): String {
        // Not every chapter has data-saver renditions; fall back to the originals.
        val saver = dataSaver && this.dataSaver.size == data.size
        val file = if (saver) this.dataSaver[index] else data[index]
        return "${baseUrl.trimEnd('/')}/${if (saver) "data-saver" else "data"}/$hash/$file"
    }
}
