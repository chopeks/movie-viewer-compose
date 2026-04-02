package pl.chopeks.movies.composables.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pl.chopeks.core.model.capability.ExternalSoftware

@Composable
fun SettingsExternalSoftwareCard(
	software: ExternalSoftware,
	version: String?,
	modifier: Modifier = Modifier
) {
	val isInstalled = version != null

	TriStateCard(
		modifier = modifier.fillMaxWidth().padding(vertical = 4.dp),
		state = version != null
	) {
		Row(
			modifier = Modifier.padding(12.dp),
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.spacedBy(16.dp)
		) {
			Box(
				modifier = Modifier
					.size(40.dp)
					.background(Color(0xFF333333), CircleShape),
				contentAlignment = Alignment.Center
			) {
				Text(
					text = software.visibleName.take(1).uppercase(),
					color = Color.White,
					fontWeight = FontWeight.Bold
				)
			}
			Column {
				Text(
					text = software.visibleName,
					style = MaterialTheme.typography.body1,
					color = Color.White
				)
				Text(
					text = version ?: "Not detected in PATH",
					style = MaterialTheme.typography.body2,
					fontFamily = FontFamily.Monospace,
					color = if (isInstalled) Color(0xFF81C784) else Color(0xFFE57373)
				)
			}
		}
	}
}