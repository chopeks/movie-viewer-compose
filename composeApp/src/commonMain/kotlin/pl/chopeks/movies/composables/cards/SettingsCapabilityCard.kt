package pl.chopeks.movies.composables.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun SettingsCapabilityCard(
	name: String,
	description: String,
	isAvailable: Boolean,
//	icon: ImageVector,
	modifier: Modifier = Modifier
) {
	TriStateCard(
		modifier = modifier,
		state = isAvailable,
	) {
		Row(
			modifier = Modifier.padding(16.dp),
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.spacedBy(16.dp)
		) {
//			Icon(
//				imageVector = icon,
//				contentDescription = null,
//				tint = if (isAvailable) Color(0xFF4CAF50) else Color(0xFFCF6679),
//				modifier = Modifier.size(24.dp)
//			)
			Column(modifier = Modifier.weight(1f)) {
				Text(
					text = name,
					style = MaterialTheme.typography.subtitle1,
					color = Color.White
				)
				Text(
					text = description,
					style = MaterialTheme.typography.caption,
					color = Color.Gray
				)
			}

			Surface(
				color = if (isAvailable) Color(0x154CAF50) else Color(0x15CF6679),
				shape = RoundedCornerShape(4.dp)
			) {
				Text(
					text = if (isAvailable) "READY" else "MISSING",
					modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
					style = MaterialTheme.typography.overline,
					color = if (isAvailable) Color(0xFF4CAF50) else Color(0xFFCF6679)
				)
			}
		}
	}
}