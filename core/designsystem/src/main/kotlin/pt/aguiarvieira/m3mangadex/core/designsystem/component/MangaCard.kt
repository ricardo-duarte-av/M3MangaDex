package pt.aguiarvieira.m3mangadex.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** A cover with its title underneath: the unit of every manga grid and row. */
@Composable
fun MangaCard(
    title: String,
    coverUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .clip(MaterialTheme.shapes.medium)
                .clickable(role = Role.Button, onClick = onClick)
                .padding(bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // The title already names the card for TalkBack; the cover adds nothing.
        MangaCover(url = coverUrl, contentDescription = null, modifier = Modifier.fillMaxWidth())
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 2,
            minLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 2.dp),
        )
    }
}
