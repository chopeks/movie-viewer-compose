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
import movieviewer.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.kodein.di.compose.rememberInstance
import pl.chopeks.core.model.Category
import pl.chopeks.movies.composables.DragDropImageContainer
import pl.chopeks.movies.composables.FilterBar
import pl.chopeks.movies.composables.ProgressIndicator
import pl.chopeks.movies.composables.ScreenSkeleton
import pl.chopeks.movies.composables.buttons.GreenTextButton
import pl.chopeks.movies.composables.cards.CategoryCard
import pl.chopeks.movies.composables.state.rememberAlertDialogState
import pl.chopeks.movies.utils.KeyEventManager
import pl.chopeks.movies.utils.KeyEventNavigation
import pl.chopeks.screenmodel.CategoriesScreenModel
import pl.chopeks.screenmodel.CategoriesScreenModel.Intent

class CategoriesScreen : Screen {
	@Composable
	override fun Content() {
		val screenModel = rememberScreenModel<CategoriesScreenModel>()
		val keyEventManager by rememberInstance<KeyEventManager>()
		val navigator = LocalNavigator.current
		val state by screenModel.state.collectAsState()

		val addDialogState = rememberAlertDialogState()
		var editingCategory by remember { mutableStateOf<Category?>(null) }

		DisposableEffect(keyEventManager, navigator) {
			keyEventManager.setListener { KeyEventNavigation.onKeyEvent(it, navigator) }
			onDispose { keyEventManager.setListener(null) }
		}

		LaunchedEffect(Unit) {
			screenModel.handleIntent(Intent.LoadCategories)
		}

		ScreenSkeleton(
			title = stringResource(Res.string.screen_categories),
			leftActions = {
				GreenTextButton(stringResource(Res.string.button_add_category), onClick = {
					addDialogState.show()
				})
			},
			rightActions = {
				FilterBar(
					query = state.searchFilter,
					onQueryChange = { screenModel.handleIntent(Intent.UpdateSearch(it)) },
				)
			}
		) {
			if (state.isLoading) {
				ProgressIndicator()
			} else {
				LazyVerticalGrid(
					columns = GridCells.Fixed(6),
					modifier = Modifier.fillMaxSize(),
					horizontalArrangement = Arrangement.spacedBy(2.dp),
					verticalArrangement = Arrangement.spacedBy(2.dp)
				) {
					items(items = state.categories, key = { it.id }) { category ->
						CategoryCard(
							category = category,
							onClick = {
								navigator?.replace(VideosScreen(category = it))
							},
							onEditClick = {
								editingCategory = it
							}
						)
					}
				}
			}
		}

		AddCategoryDialog(
			isVisible = addDialogState.isVisible,
			onDismiss = { addDialogState.hide() },
			onConfirm = { name, bytes ->
				screenModel.handleIntent(Intent.AddCategory(name, bytes))
				addDialogState.hide()
			}
		)

		EditCategoryDialog(
			category = editingCategory,
			onDismiss = { editingCategory = null },
			onConfirm = { name, bytes ->
				editingCategory?.let { screenModel.handleIntent(Intent.EditCategory(it, name, bytes)) }
				editingCategory = null
			},
			onRemove = {
				editingCategory?.let { screenModel.handleIntent(Intent.RemoveCategory(it)) }
				editingCategory = null
			}
		)
	}

	@Composable
	private fun AddCategoryDialog(
		isVisible: Boolean,
		onDismiss: () -> Unit,
		onConfirm: (String, ByteArray?) -> Unit
	) {
		if (isVisible) {
			var name by remember { mutableStateOf("") }
			var droppedImageBytes by remember { mutableStateOf<ByteArray?>(null) }

			AlertDialog(
				onDismissRequest = onDismiss,
				title = { Text(stringResource(Res.string.button_add_category)) },
				text = {
					Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
						TextField(
							value = name,
							onValueChange = { name = it },
							label = { Text(stringResource(Res.string.label_name)) },
							modifier = Modifier.fillMaxWidth()
						)
						DragDropImageContainer(
							ratio = 1.77f,
							url = "",
							imageBytes = null,
							droppedImageBytes = droppedImageBytes,
							onDroppedImageBytesChanged = { droppedImageBytes = it }
						)
					}
				}, confirmButton = {
					Button(
						onClick = { if (name.isNotBlank()) onConfirm(name, droppedImageBytes) },
						colors = ButtonDefaults.buttonColors(backgroundColor = Color.Black)
					) {
						Text(stringResource(Res.string.button_add), color = Color.White)
					}
				}, dismissButton = {
					Button(
						onClick = onDismiss,
						colors = ButtonDefaults.buttonColors(backgroundColor = Color.Black)
					) {
						Text(stringResource(Res.string.button_cancel), color = Color.LightGray)
					}
				}
			)
		}
	}

	@Composable
	private fun EditCategoryDialog(
		category: Category?,
		onDismiss: () -> Unit,
		onConfirm: (String, ByteArray?) -> Unit,
		onRemove: () -> Unit
	) {
		if (category != null) {
			var name by remember { mutableStateOf(category.name) }
			var droppedImageBytes by remember { mutableStateOf<ByteArray?>(null) }

			AlertDialog(
				onDismissRequest = onDismiss,
				title = { Text(stringResource(Res.string.label_edit_category)) },
				text = {
					Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
						TextField(
							value = name,
							onValueChange = { name = it },
							label = { Text(stringResource(Res.string.label_name)) },
							modifier = Modifier.fillMaxWidth()
						)
						DragDropImageContainer(
							ratio = 1.77f,
							url = if ((category.image ?: "").startsWith("http")) category.image ?: "" else "",
							imageBytes = category.imageBytes,
							droppedImageBytes = droppedImageBytes,
							onDroppedImageBytesChanged = { droppedImageBytes = it }
						)
					}
				}, confirmButton = {
					Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
						Button(
							onClick = onRemove,
							colors = ButtonDefaults.buttonColors(backgroundColor = Color.Black)
						) {
							Text(stringResource(Res.string.button_remove), color = Color.Red)
						}
						Button(
							onClick = onDismiss,
							colors = ButtonDefaults.buttonColors(backgroundColor = Color.Black)
						) {
							Text(stringResource(Res.string.button_cancel), color = Color.LightGray)
						}
						Button(
							onClick = { if (name.isNotBlank()) onConfirm(name, droppedImageBytes) },
							colors = ButtonDefaults.buttonColors(backgroundColor = Color.Black)
						) {
							Text(stringResource(Res.string.button_confirm), color = Color.White)
						}
					}
				}
			)
		}
	}
}
