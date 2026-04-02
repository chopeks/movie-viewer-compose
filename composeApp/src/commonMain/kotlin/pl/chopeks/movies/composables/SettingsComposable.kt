package pl.chopeks.movies.composables

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import movieviewer.composeapp.generated.resources.Res
import movieviewer.composeapp.generated.resources.button_desc_delete
import org.jetbrains.compose.resources.stringResource
import pl.chopeks.core.model.Path

@Composable
fun SettingsHeaderText(text: String) {
	Text(text, modifier = Modifier.padding(8.dp), style = MaterialTheme.typography.h6)
}

@Composable
fun SettingsDirectory(path: Path, onRemoveClick: (Path) -> Unit) {
	Row(verticalAlignment = Alignment.CenterVertically) {
		IconButton({ onRemoveClick(path) }) {
			Icon(Icons.Default.Delete, contentDescription = stringResource(Res.string.button_desc_delete))
		}
		Text(path.path)
	}
}