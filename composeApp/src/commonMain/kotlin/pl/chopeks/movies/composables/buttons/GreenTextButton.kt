package pl.chopeks.movies.composables.buttons

import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun GreenTextButton(
	text: String,
	onClick: () -> Unit
) {
	TextButton(onClick) {
		Text(text.uppercase(), color = Color.Green.copy(alpha = 0.5f))
	}
}