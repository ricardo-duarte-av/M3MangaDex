package pt.aguiarvieira.m3mangadex.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class DescriptionsTest {
    @Test
    fun `strips markdown to readable text`() {
        val md = "**Bold** and *it* with [a link](https://x.org).\r\n\r\n\r\n\r\n---\r\n## Links\r\n- [Official](https://y.org)"
        assertEquals("Bold and it with a link.\n\nLinks\n- Official", Descriptions.plain(md))
    }

    @Test
    fun `leaves list dashes and snake_case alone`() {
        assertEquals("- item\nsome_name here", Descriptions.plain("- item\nsome_name here"))
    }
}
