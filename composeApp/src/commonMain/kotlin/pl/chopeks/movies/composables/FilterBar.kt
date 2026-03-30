package pl.chopeks.movies.composables

import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import movieviewer.composeapp.generated.resources.Res
import movieviewer.composeapp.generated.resources.button_desc_delete
import movieviewer.composeapp.generated.resources.button_desc_search
import movieviewer.composeapp.generated.resources.label_filter
import org.jetbrains.compose.resources.stringResource

@Composable
fun FilterBar(
	query: String,
	onQueryChange: (String) -> Unit,
	modifier: Modifier = Modifier
) {
	StyledTextField(
		modifier = modifier.testTag("textField"),
		value = query,
		onValueChange = onQueryChange,
		trailingIcon = {
			if (query.isEmpty())
				Icon(Icons.Default.Search, contentDescription = stringResource(Res.string.button_desc_search))
			else
				IconButton(onClick = { onQueryChange("") }, modifier = Modifier.testTag("deleteButton")) {
					Icon(Icons.Default.Delete, contentDescription = stringResource(Res.string.button_desc_delete))
				}
		},
		label = stringResource(Res.string.label_filter),
	)
}
