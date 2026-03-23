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
import pl.chopeks.core.model.Actor
import pl.chopeks.movies.composables.FilterBar
import pl.chopeks.movies.composables.ScreenSkeleton
import pl.chopeks.movies.composables.buttons.GreenTextButton
import pl.chopeks.movies.composables.cards.ActorCard
import pl.chopeks.movies.composables.state.AlertDialogState
import pl.chopeks.movies.composables.state.rememberAlertDialogState
import pl.chopeks.movies.internal.screenmodel.ActorsScreenModel
import pl.chopeks.movies.utils.KeyEventManager
import pl.chopeks.movies.utils.KeyEventNavigation

class ActorsScreen : Screen {
	@Composable
	override fun Content() {
		val scope = rememberCoroutineScope()
		val addDialog = rememberAlertDialogState()
		val editDialog = remember { mutableStateOf<Actor?>(null) }
		val screenModel = rememberScreenModel<ActorsScreenModel>()
		val keyEventManager = localDI().direct.instance<KeyEventManager>()
		val navigator = LocalNavigator.current
		keyEventManager.setListener { KeyEventNavigation.onKeyEvent(it, navigator) }

		ScreenSkeleton(
			title = stringResource(Res.string.screen_actors),
			leftActions = {
				GreenTextButton(stringResource(Res.string.button_add_actor), onClick = {
					scope.launch { addDialog.show() }
				})
			},
			rightActions = {
				FilterBar(
					query = screenModel.searchFilter,
					onQueryChange = { screenModel.searchFilter = it }
				)
			}
		) { scope ->
			val actors by screenModel.filteredActors.collectAsState()

			BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
				val sidePadding = maxWidth * 0.08f
				LazyVerticalGrid(
					columns = GridCells.Fixed(8),
					modifier = Modifier.fillMaxSize(),
					contentPadding = PaddingValues(sidePadding, 0.dp),
					horizontalArrangement = Arrangement.spacedBy(2.dp),
					verticalArrangement = Arrangement.spacedBy(2.dp)
				) {
					items(items = actors, key = Actor::id) { actor ->
						ActorCard(
							actor = actor,
							onClick = {
								scope.launch {
									navigator?.replace(VideosScreen(actor = it))
								}
							},
							onEditClick = {
								editDialog.value = it
							}
						)
					}
				}
			}
		}

		AddActorDialog(addDialog, screenModel)
		EditActorDialog(editDialog, screenModel)

		LaunchedEffect(Unit) {
			screenModel.getActors()
		}
	}

	@Composable
	fun AddActorDialog(dialogState: AlertDialogState, screenModel: ActorsScreenModel) {
		if (dialogState.isVisible) {
			val scope = rememberCoroutineScope()
			val context = LocalPlatformContext.current
			var name by remember { mutableStateOf("") }
			var url by remember { mutableStateOf("") }
			AlertDialog(
				onDismissRequest = { scope.launch { dialogState.hide() } },
				title = { Text(stringResource(Res.string.button_add_actor)) },
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
				},
				confirmButton = {
					Button(onClick = {
						if (name.isNotBlank()) {
							screenModel.add(name, url)
							scope.launch { dialogState.hide() }
						}
					}, colors = ButtonDefaults.buttonColors(backgroundColor = Color.Black)) {
						Text(stringResource(Res.string.button_add), color = Color.White)
					}
				},
				dismissButton = {
					Button(onClick = { scope.launch { dialogState.hide() } }, colors = ButtonDefaults.buttonColors(backgroundColor = Color.Black)) {
						Text(stringResource(Res.string.button_cancel), color = Color.LightGray)
					}
				}
			)
		}
	}

	@Composable
	fun EditActorDialog(actor: MutableState<Actor?>, screenModel: ActorsScreenModel) {
		if (actor.value != null) {
			val context = LocalPlatformContext.current
			var name by remember { mutableStateOf(actor.value!!.name) }
			var url by remember { mutableStateOf(if ((actor.value!!.image ?: "").startsWith("http:")) actor.value!!.image ?: "" else "") }
			AlertDialog(
				onDismissRequest = { actor.value = null },
				title = { Text(stringResource(Res.string.label_edit_actor)) },
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
				},
				confirmButton = {
					Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
						Button(onClick = {
							screenModel.dedup(actor.value!!)
							actor.value = null
						}, colors = ButtonDefaults.buttonColors(backgroundColor = Color.Black)) {
							Text(stringResource(Res.string.button_deduplicate), color = Color.LightGray)
						}
						Button(onClick = {
							screenModel.remove(actor.value!!)
							actor.value = null
						}, colors = ButtonDefaults.buttonColors(backgroundColor = Color.Black)) {
							Text(stringResource(Res.string.button_remove), color = Color.Red)
						}

						Button(onClick = { actor.value = null }, colors = ButtonDefaults.buttonColors(backgroundColor = Color.Black)) {
							Text(stringResource(Res.string.button_cancel), color = Color.LightGray)
						}

						Button(onClick = {
							if (name.isNotBlank()) {
								screenModel.edit(actor.value!!, name, url)
								actor.value = null
							}
						}, colors = ButtonDefaults.buttonColors(backgroundColor = Color.Black)) {
							Text(stringResource(Res.string.button_confirm), color = Color.White)
						}
					}
				}
			)
		}
	}
}