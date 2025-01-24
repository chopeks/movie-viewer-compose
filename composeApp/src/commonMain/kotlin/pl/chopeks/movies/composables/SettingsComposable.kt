package pl.chopeks.movies.composables

import androidx.compose.foundation.layout.Row
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import pl.chopeks.movies.model.Path

@Composable
fun SettingsHeaderText(text: String) {
  Text(text, fontSize = TextUnit(18f, TextUnitType.Sp))
}

@Composable
fun SettingsDirectory(path: Path, onRemove: (Path) -> Unit) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    IconButton({ onRemove(path) }) {
      Icon(Icons.Default.Delete, "remove")
    }
    Text(path.path)
  }
}