package pt.aguiarvieira.m3mangadex.feature.manga

import org.junit.Assert.assertEquals
import org.junit.Test
import pt.aguiarvieira.m3mangadex.core.model.Chapter
import pt.aguiarvieira.m3mangadex.core.model.ChapterProgress
import java.time.Instant

class ResumeChapterTest {
    private val chapters = listOf("1", "2", "3").map { Chapter("c$it", "m", null, it, null, "en", 20, null, null, emptyList()) }

    private fun progress(
        chapter: String,
        page: Int,
    ) = ChapterProgress("c$chapter", "m", chapter, page, pageCount = 20, readAt = Instant.EPOCH)

    @Test
    fun `nothing read starts at the first chapter`() {
        assertEquals("c1", resumeChapter(chapters, null)?.id)
    }

    @Test
    fun `a half-read chapter is continued`() {
        assertEquals("c2", resumeChapter(chapters, progress("2", page = 5))?.id)
    }

    @Test
    fun `a finished chapter moves on, or stays when it is the latest`() {
        assertEquals("c3", resumeChapter(chapters, progress("2", page = 19))?.id)
        assertEquals("c3", resumeChapter(chapters, progress("3", page = 19))?.id)
    }
}
