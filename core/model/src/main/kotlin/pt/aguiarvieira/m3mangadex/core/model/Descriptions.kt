package pt.aguiarvieira.m3mangadex.core.model

/**
 * MangaDex descriptions are Markdown, often with trailing link lists and separators. This reduces
 * them to readable plain text: links become their label, emphasis markers and rules go, and runs
 * of blank lines collapse.
 */
object Descriptions {
    private val link = Regex("""\[([^\]]*)]\((?:[^)]*)\)""")
    private val emphasis = Regex("""(\*\*|__|\*|_|~~)(?=\S)(.+?)(?<=\S)\1""")
    private val rule = Regex("""(?m)^\s*([-*_]\s*){3,}$""")
    private val heading = Regex("""(?m)^#{1,6}\s*""")
    private val blankLines = Regex("""\n{3,}""")

    fun plain(markdown: String): String =
        markdown
            .replace("\r\n", "\n")
            .replace(link, "$1")
            .replace(rule, "")
            .replace(heading, "")
            .replace(emphasis, "$2")
            .replace(blankLines, "\n\n")
            .trim()
}
