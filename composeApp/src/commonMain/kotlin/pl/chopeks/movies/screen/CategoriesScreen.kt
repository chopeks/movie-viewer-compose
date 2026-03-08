package pl.chopeks.movies.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
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
import org.kodein.di.compose.localDI
import org.kodein.di.direct
import org.kodein.di.instance
import pl.chopeks.core.model.Actor
import pl.chopeks.movies.composables.ScreenSkeleton
import pl.chopeks.movies.composables.cards.CategoryCard
import pl.chopeks.movies.internal.screenmodel.CategoriesScreenModel
import pl.chopeks.core.model.Category
import pl.chopeks.movies.composables.cards.ActorCard
import pl.chopeks.movies.utils.KeyEventManager
import pl.chopeks.movies.utils.KeyEventNavigation

class CategoriesScreen : Screen {
	@Composable
	override fun Content() {
		val addDialog = remember { mutableStateOf(false) }
		val editDialog = remember { mutableStateOf<Category?>(null) }
		val screenModel = rememberScreenModel<CategoriesScreenModel>()
		val keyEventManager = localDI().direct.instance<KeyEventManager>()
		val navigator = LocalNavigator.current
		keyEventManager.setListener { KeyEventNavigation.onKeyEvent(it, navigator) }

		ScreenSkeleton(
			title = "Categories",
			textActions = {
				TextButton({
					addDialog.value = true
				}) { Text("Add category".uppercase(), color = Color.Green.copy(alpha = 0.5f)) }
			},
			actions = {
				TextField(
					screenModel.searchFilter, { screenModel.searchFilter = it },
					trailingIcon = {
						if (screenModel.searchFilter.isEmpty())
							Icon(Icons.Default.Search, contentDescription = "Search")
						else
							IconButton(onClick = { screenModel.searchFilter = "" }) {
								Icon(Icons.Default.Delete, contentDescription = "Delete")
							}
					},
					label = { Text("Filter") },
					colors = TextFieldDefaults.textFieldColors(backgroundColor = Color.Black)
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
	fun AddCategoryDialog(show: MutableState<Boolean>, screenModel: CategoriesScreenModel) {
		if (show.value) {
			val context = LocalPlatformContext.current
			var name by remember { mutableStateOf("") }
			var url by remember { mutableStateOf("") }
			AlertDialog(onDismissRequest = { show.value = false }, title = { Text("Add category") }, text = {
				Column(Modifier.fillMaxWidth()) {
					TextField(name, { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
					TextField(url, { url = it }, label = { Text("Image url") }, modifier = Modifier.fillMaxWidth())
					AsyncImage(
						ImageRequest.Builder(context).data(url).size(Size.ORIGINAL).build(), null, Modifier.fillMaxWidth().aspectRatio(1.77f)
					)
				}
			}, confirmButton = {
				Button(onClick = {
					if (name.isNotBlank()) {
						screenModel.add(name, url)
						show.value = false
					}
				}, colors = ButtonDefaults.buttonColors(backgroundColor = Color.Black)) {
					Text("Add", color = Color.White)
				}
			}, dismissButton = {
				Button(onClick = { show.value = false }, colors = ButtonDefaults.buttonColors(backgroundColor = Color.Black)) {
					Text("Cancel", color = Color.LightGray)
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
			AlertDialog(onDismissRequest = { category.value = null }, title = { Text("Edit category") }, text = {
				Column(Modifier.fillMaxWidth()) {
					TextField(name, { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
					TextField(url, { url = it }, label = { Text("Image url") }, modifier = Modifier.fillMaxWidth())
					AsyncImage(
						ImageRequest.Builder(context).data(url).size(Size.ORIGINAL).build(), null, Modifier.fillMaxWidth().aspectRatio(1.77f)
					)
				}
			}, confirmButton = {
				Button(onClick = {
					if (name.isNotBlank()) {
						screenModel.edit(category.value!!, name, url)
						category.value = null
					}
				}, colors = ButtonDefaults.buttonColors(backgroundColor = Color.Black)) {
					Text("Confirm", color = Color.White)
				}
			}, dismissButton = {
				Button(onClick = { category.value = null }, colors = ButtonDefaults.buttonColors(backgroundColor = Color.Black)) {
					Text("Cancel", color = Color.LightGray)
				}
			})
		}
	}
}