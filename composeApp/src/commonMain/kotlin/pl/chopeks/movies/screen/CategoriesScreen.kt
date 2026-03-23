package pl.chopeks.movies.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.kodein.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.size.Size
import kotlinx.coroutines.launch
import movieviewer.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.kodein.di.compose.localDI
import org.kodein.di.direct
import org.kodein.di.instance
import pl.chopeks.core.model.Category
import pl.chopeks.movies.composables.FilterBar
import pl.chopeks.movies.composables.ScreenSkeleton
import pl.chopeks.movies.composables.buttons.GreenTextButton
import pl.chopeks.movies.composables.cards.CategoryCard
import pl.chopeks.movies.composables.state.AlertDialogState
import pl.chopeks.movies.composables.state.rememberAlertDialogState
import pl.chopeks.movies.internal.screenmodel.CategoriesScreenModel
import pl.chopeks.movies.utils.KeyEventManager
import pl.chopeks.movies.utils.KeyEventNavigation

class CategoriesScreen : Screen {
	@Composable
	override fun Content() {
		val scope = rememberCoroutineScope()
		val addDialog = rememberAlertDialogState()
		val editDialog = remember { mutableStateOf<Category?>(null) }
		val screenModel = rememberScreenModel<CategoriesScreenModel>()
		val keyEventManager = localDI().direct.instance<KeyEventManager>()
		val navigator = LocalNavigator.current
		keyEventManager.setListener { KeyEventNavigation.onKeyEvent(it, navigator) }

		ScreenSkeleton(
			title = stringResource(Res.string.screen_categories),
			leftActions = {
				GreenTextButton(stringResource(Res.string.button_add_category), onClick = { scope.launch { addDialog.show() } })
			},
			rightActions = {
				FilterBar(
					query = screenModel.searchFilter,
					onQueryChange = { screenModel.searchFilter = it },
				)
			}
		) { scope ->
			val categories by screenModel.filteredCategories.collectAsState()
			LazyVerticalGrid(
				columns = GridCells.Fixed(6),
				modifier = Modifier.fillMaxSize(),
				horizontalArrangement = Arrangement.spacedBy(2.dp),
				verticalArrangement = Arrangement.spacedBy(2.dp)
			) {
				items(items = categories, key = Category::id) { category ->
					CategoryCard(
						category = category,
						onClick = {
							scope.launch {
								navigator?.replace(VideosScreen(category = it))
							}
						},
						onEditClick = {
							editDialog.value = it
						}
					)
				}
			}
		}

		AddCategoryDialog(addDialog, screenModel)
		EditCategoryDialog(editDialog, screenModel)

		LaunchedEffect(Unit) {
			screenModel.getCategories()
		}
	}

	@Composable
	fun AddCategoryDialog(dialogState: AlertDialogState, screenModel: CategoriesScreenModel) {
		if (dialogState.isVisible) {
			val scope = rememberCoroutineScope()
			val context = LocalPlatformContext.current
			var name by remember { mutableStateOf("") }
			var url by remember { mutableStateOf("") }
			AlertDialog(
				onDismissRequest = { scope.launch { dialogState.hide() } },
				title = { Text(stringResource(Res.string.button_add_category)) },
				text = {
					Column(Modifier.fillMaxWidth()) {
						TextField(name, { name = it }, label = { Text(stringResource(Res.string.label_name)) }, modifier = Modifier.fillMaxWidth())
						TextField(url, { url = it }, label = { Text(stringResource(Res.string.label_image_url)) }, modifier = Modifier.fillMaxWidth())
						AsyncImage(
							ImageRequest.Builder(context)
								.data(url)
								.size(Size.ORIGINAL)
								.build(), null, Modifier.fillMaxWidth().aspectRatio(1.77f)
						)
					}
				}, confirmButton = {
					Button(onClick = {
						if (name.isNotBlank()) {
							screenModel.add(name, url)
							scope.launch { dialogState.hide() }
						}
					}, colors = ButtonDefaults.buttonColors(backgroundColor = Color.Black)) {
						Text(stringResource(Res.string.button_add), color = Color.White)
					}
				}, dismissButton = {
					Button(onClick = { scope.launch { dialogState.hide() } }, colors = ButtonDefaults.buttonColors(backgroundColor = Color.Black)) {
						Text(stringResource(Res.string.button_cancel), color = Color.LightGray)
					}
				})
		}
	}

	@Composable
	fun EditCategoryDialog(category: MutableState<Category?>, screenModel: CategoriesScreenModel) {
		if (category.value != null) {
			val context = LocalPlatformContext.current
			var name by remember { mutableStateOf(category.value!!.name) }
			var url by remember { mutableStateOf(if ((category.value!!.image ?: "").startsWith("http:")) category.value!!.image ?: "" else "") }
			AlertDialog(onDismissRequest = { category.value = null }, title = { Text(stringResource(Res.string.label_edit_category)) }, text = {
				Column(Modifier.fillMaxWidth()) {
					TextField(name, { name = it }, label = { Text(stringResource(Res.string.label_name)) }, modifier = Modifier.fillMaxWidth())
					TextField(url, { url = it }, label = { Text(stringResource(Res.string.label_image_url)) }, modifier = Modifier.fillMaxWidth())
					AsyncImage(
						ImageRequest.Builder(context).data(url).size(Size.ORIGINAL).build(), null, Modifier.fillMaxWidth().aspectRatio(1.77f)
					)
				}
			}, confirmButton = {
				Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
					Button(onClick = {
						screenModel.remove(category.value!!)
						category.value = null
					}, colors = ButtonDefaults.buttonColors(backgroundColor = Color.Black)) {
						Text(stringResource(Res.string.button_remove), color = Color.Red)
					}

					Button(onClick = { category.value = null }, colors = ButtonDefaults.buttonColors(backgroundColor = Color.Black)) {
						Text(stringResource(Res.string.button_cancel), color = Color.LightGray)
					}

					Button(onClick = {
						if (name.isNotBlank()) {
							screenModel.edit(category.value!!, name, url)
							category.value = null
						}
					}, colors = ButtonDefaults.buttonColors(backgroundColor = Color.Black)) {
						Text(stringResource(Res.string.button_confirm), color = Color.White)
					}
				}

			})
		}
	}
}