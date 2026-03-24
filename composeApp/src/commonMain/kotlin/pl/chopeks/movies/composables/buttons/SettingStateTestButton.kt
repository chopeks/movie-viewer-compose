package pl.chopeks.movies.composables.buttons

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import movieviewer.composeapp.generated.resources.Res
import movieviewer.composeapp.generated.resources.label_state_found
import movieviewer.composeapp.generated.resources.label_state_not_found
import movieviewer.composeapp.generated.resources.label_state_unknown
import org.jetbrains.compose.resources.stringResource

@Composable
fun SettingStateTestButton(
	labelText: String,
	buttonText: String,
	status: Boolean?,
	modifier: Modifier = Modifier,
	onClick: () -> Unit
) {
	Row(modifier = modifier.width(300.dp), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
		Text(labelText, modifier = Modifier.weight(1f, fill = true).padding(8.dp))
		Text(
			when (status) {
				true -> stringResource(Res.string.label_state_found)
				false -> stringResource(Res.string.label_state_not_found)
				null -> stringResource(Res.string.label_state_unknown)
			},
			color = when (status) {
				true -> Color.Green.copy(alpha = 0.5f)
				false -> Color.Red.copy(alpha = 0.5f)
				null -> Color.Gray
			},
			modifier = Modifier.padding(horizontal = 8.dp)
		)
		Button(onClick) {
			Text(buttonText)
		}
	}
}