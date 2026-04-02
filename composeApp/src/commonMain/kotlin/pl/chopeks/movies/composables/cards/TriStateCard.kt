package pl.chopeks.movies.composables.cards

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun TriStateCard(
	state: Boolean?,
	modifier: Modifier = Modifier,
	content: @Composable () -> Unit
) {
	Card(
		modifier = modifier.fillMaxWidth().padding(vertical = 4.dp),
		elevation = 2.dp,
		backgroundColor = Color(0xFF1E1E1E), // Dark Gray background
		shape = RoundedCornerShape(8.dp),
		border = BorderStroke(
			1.dp, when (state) {
				true -> Color(0xFF4CAF50)
				false -> Color(0xFFCF6679)
				else ->  Color.Gray
			}
		),
		content = content
	)
}