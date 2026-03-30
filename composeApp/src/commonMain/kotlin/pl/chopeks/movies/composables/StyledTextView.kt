package pl.chopeks.movies.composables

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun StyledTextField(
	value: String,
	onValueChange: (String) -> Unit,
	label: String,
	trailingIcon: @Composable (() -> Unit)? = null,
	modifier: Modifier = Modifier
) {
	TextField(
		value = value,
		onValueChange = onValueChange,
		label = { Text(label) },
		modifier = modifier,
		shape = RoundedCornerShape(8.dp),
		trailingIcon = trailingIcon,
		colors = TextFieldDefaults.textFieldColors(
			textColor = Color.White,
			backgroundColor = Color.White.copy(alpha = 0.1f),
			cursorColor = Color.White,
			focusedIndicatorColor = Color.Transparent,
			unfocusedIndicatorColor = Color.Transparent,
			disabledIndicatorColor = Color.Transparent,
			focusedLabelColor = Color.LightGray,
			unfocusedLabelColor = Color.Gray
		)
	)
}