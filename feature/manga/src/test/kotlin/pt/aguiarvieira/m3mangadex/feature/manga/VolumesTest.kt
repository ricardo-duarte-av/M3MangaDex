package pt.aguiarvieira.m3mangadex.feature.manga

import org.junit.Assert.assertEquals
import org.junit.Test
import pt.aguiarvieira.m3mangadex.core.model.Chapter

class VolumesTest {
    private fun chapter(
        volume: String?,
        number: String?,
        external: Boolean = false,
    ) = Chapter(
        id = "$volume-$number",
        mangaId = "m",
        volume = volume,
        number = number,
        title = null,
        language = "en",
        pages = if (external) 0 else 20,
        externalUrl = if (external) "https://x" else null,
        readableAt = null,
        groups = emptyList(),
    )

    @Test
    fun `unvolumed first, then volumes newest first, chapters numerically descending`() {
        val groups =
            groupByVolume(
                listOf(chapter("1", "2"), chapter("1", "10"), chapter("2", "11"), chapter(null, "20"), chapter("10", "50"), chapter("1", "1.5")),
            )
        assertEquals(listOf(null, "10", "2", "1"), groups.map { it.volume })
        assertEquals(listOf("10", "2", "1.5"), groups.last().chapters.map { it.number })
    }

    @Test
    fun `first readable skips external chapters`() {
        val first = firstReadable(listOf(chapter("1", "1", external = true), chapter("1", "2"), chapter("1", "3")))
        assertEquals("2", first?.number)
    }

    @Test
    fun `first readable falls back to external chapters when that's all there is`() {
        val first = firstReadable(listOf(chapter("1", "2", external = true), chapter("1", "1", external = true)))
        assertEquals("1", first?.number)
    }
}
