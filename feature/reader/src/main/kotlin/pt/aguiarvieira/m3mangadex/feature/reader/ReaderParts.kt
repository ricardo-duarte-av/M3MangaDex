package pt.aguiarvieira.m3mangadex.feature.reader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import pt.aguiarvieira.m3mangadex.core.designsystem.component.ErrorMessage
import pt.aguiarvieira.m3mangadex.core.model.Chapter
import pt.aguiarvieira.m3mangadex.core.model.ChapterNeighbors

@Composable
internal fun PageFailed(
    page: Int,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier, contentAlignment = Alignment.Center) {
        ErrorMessage(onRetry = onRetry, message = stringResource(R.string.reader_page_failed, page + 1))
    }
}

/** "Ch. 12 · Title", or the oneshot label. */
@Composable
internal fun Chapter.label(): String {
    val number = number?.let { stringResource(R.string.reader_chapter, it) } ?: stringResource(R.string.reader_oneshot)
    return listOfNotNull(number, title).joinToString(" · ")
}

/**
 * After the last page: what was read (crediting its translators, as MangaDex asks), and the way
 * on to the next chapter, or back when there's none yet.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun EndOfChapter(
    chapter: Chapter,
    neighbors: ChapterNeighbors,
    onOpenChapter: (Chapter) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.widthIn(max = 480.dp),
        ) {
            Text(
                stringResource(R.string.reader_end_title, chapter.label()),
                style = MaterialTheme.typography.headlineSmallEmphasized,
                color = Color.White,
                textAlign = TextAlign.Center,
            )
            if (chapter.groups.isNotEmpty()) {
                Text(
                    stringResource(R.string.reader_credit, chapter.groups.joinToString { it.name }),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                )
            }
            val next = neighbors.next
            if (next != null) {
                val label = next.label()
                Button(onClick = { onOpenChapter(next) }) {
                    Text(
                        if (next.isExternal) {
                            stringResource(R.string.reader_next_external, label)
                        } else {
                            stringResource(R.string.reader_next, label)
                        },
                    )
                }
            } else {
                Text(
                    stringResource(R.string.reader_up_to_date),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                )
            }
            OutlinedButton(onClick = onBack) { Text(stringResource(R.string.reader_back_to_details)) }
        }
    }
}
