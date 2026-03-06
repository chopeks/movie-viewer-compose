package pl.chopeks.movies.composables.buttons

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DrawerButton(text: String, shortcut: String? = null, onClick: () -> Unit) {
	Row (modifier = Modifier
		.fillMaxWidth()
		.clickable(onClick = onClick)
		.padding(16.dp)) {
		Text(
			text = text,
			modifier = Modifier.width(200.dp).align(Alignment.CenterVertically),
			color = Color.White
		)
		if (shortcut != null) {
			Text(
				text = shortcut,
				fontSize = 10.sp,
				modifier = Modifier.align(Alignment.CenterVertically),
				color = Color.Gray
			)
		}
	}
}
