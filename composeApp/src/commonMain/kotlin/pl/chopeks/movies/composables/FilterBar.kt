package pl.chopeks.movies.composables

import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag

@Composable
fun FilterBar(
	query: String,
	onQueryChange: (String) -> Unit,
	modifier: Modifier = Modifier
) {
	TextField(
		modifier = modifier.testTag("textField"),
		value = query,
		onValueChange = onQueryChange,
		trailingIcon = {
			if (query.isEmpty())
				Icon(Icons.Default.Search, contentDescription = "Search")
			else
				IconButton(onClick = { onQueryChange("") }, modifier = Modifier.testTag("deleteButton")) {
					Icon(Icons.Default.Delete, contentDescription = "Delete")
				}
		},
		label = { Text("Filter") },
		colors = TextFieldDefaults.textFieldColors(backgroundColor = Color.Black, textColor = Color.White)
	)
}
