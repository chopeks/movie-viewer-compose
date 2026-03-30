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
import coil3.request.crossfade
import coil3.size.Size
import movieviewer.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.kodein.di.compose.rememberInstance
import pl.chopeks.core.model.Actor
import pl.chopeks.movies.composables.FilterBar
import pl.chopeks.movies.composables.ProgressIndicator
import pl.chopeks.movies.composables.ScreenSkeleton
import pl.chopeks.movies.composables.buttons.GreenTextButton
import pl.chopeks.movies.composables.cards.ActorCard
import pl.chopeks.movies.composables.state.rememberAlertDialogState
import pl.chopeks.movies.utils.KeyEventManager
import pl.chopeks.movies.utils.KeyEventNavigation
import pl.chopeks.screenmodel.ActorsScreenModel
import pl.chopeks.screenmodel.ActorsScreenModel.Intent

class ActorsScreen : Screen {
	@Composable
	override fun Content() {
		val screenModel = rememberScreenModel<ActorsScreenModel>()
		val keyEventManager by rememberInstance<KeyEventManager>()
		val navigator = LocalNavigator.current
		val state by screenModel.state.collectAsState()

		val addDialogState = rememberAlertDialogState()
		var editingActor by remember { mutableStateOf<Actor?>(null) }

		DisposableEffect(keyEventManager, navigator) {
			keyEventManager.setListener { KeyEventNavigation.onKeyEvent(it, navigator) }
			onDispose { keyEventManager.setListener(null) }
		}

		LaunchedEffect(Unit) {
			screenModel.handleIntent(Intent.LoadActors)
		}

		ScreenSkeleton(
			title = stringResource(Res.string.screen_actors),
			leftActions = {
				GreenTextButton(stringResource(Res.string.button_add_actor), onClick = {
					addDialogState.show()
				})
			},
			rightActions = {
				FilterBar(
					query = state.searchFilter,
					onQueryChange = { screenModel.handleIntent(Intent.UpdateSearch(it)) }
				)
			}
		) {
			if (state.isLoading) {
				ProgressIndicator()
			} else {
				BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
					val sidePadding = maxWidth * 0.08f
					LazyVerticalGrid(
						columns = GridCells.Fixed(8),
						modifier = Modifier.fillMaxSize(),
						contentPadding = PaddingValues(sidePadding, 0.dp),
						horizontalArrangement = Arrangement.spacedBy(2.dp),
						verticalArrangement = Arrangement.spacedBy(2.dp)
					) {
						items(items = state.actors, key = { it.id }) { actor ->
							ActorCard(
								actor = actor,
								onClick = {
									navigator?.replace(VideosScreen(actor = it))
								},
								onEditClick = {
									editingActor = it
								}
							)
						}
					}
				}
			}
		}

		AddActorDialog(
			isVisible = addDialogState.isVisible,
			onDismiss = { addDialogState.hide() },
			onConfirm = { name, url ->
				screenModel.handleIntent(Intent.AddActor(name, url))
				addDialogState.hide()
			}
		)

		EditActorDialog(
			actor = editingActor,
			onDismiss = { editingActor = null },
			onConfirm = { name, url ->
				editingActor?.let { screenModel.handleIntent(Intent.EditActor(it, name, url)) }
				editingActor = null
			},
			onRemove = {
				editingActor?.let { screenModel.handleIntent(Intent.RemoveActor(it)) }
				editingActor = null
			},
			onDeduplicate = {
				editingActor?.let { screenModel.handleIntent(Intent.Deduplicate(it)) }
				editingActor = null
			}
		)
	}

	@Composable
	private fun AddActorDialog(
		isVisible: Boolean,
		onDismiss: () -> Unit,
		onConfirm: (String, String) -> Unit
	) {
		if (isVisible) {
			val context = LocalPlatformContext.current
			var name by remember { mutableStateOf("") }
			var url by remember { mutableStateOf("") }

			AlertDialog(
				onDismissRequest = onDismiss,
				title = { Text(stringResource(Res.string.button_add_actor)) },
				text = {
					Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
						TextField(
							value = name,
							onValueChange = { name = it },
							label = { Text(stringResource(Res.string.label_name)) },
							modifier = Modifier.fillMaxWidth()
						)
						TextField(
							value = url,
							onValueChange = { url = it },
							label = { Text(stringResource(Res.string.label_image_url)) },
							modifier = Modifier.fillMaxWidth()
						)
						if (url.isNotBlank()) {
							AsyncImage(
								model = ImageRequest.Builder(context)
									.data(url)
									.size(Size.ORIGINAL)
									.crossfade(true)
									.build(),
								contentDescription = null,
								modifier = Modifier.fillMaxWidth().aspectRatio(1.77f)
							)
						}
					}
				},
				confirmButton = {
					Button(
						onClick = { if (name.isNotBlank()) onConfirm(name, url) },
						colors = ButtonDefaults.buttonColors(backgroundColor = Color.Black)
					) {
						Text(stringResource(Res.string.button_add), color = Color.White)
					}
				},
				dismissButton = {
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
	private fun EditActorDialog(
		actor: Actor?,
		onDismiss: () -> Unit,
		onConfirm: (String, String) -> Unit,
		onRemove: () -> Unit,
		onDeduplicate: () -> Unit
	) {
		if (actor != null) {
			val context = LocalPlatformContext.current
			var name by remember { mutableStateOf(actor.name) }
			var url by remember {
				mutableStateOf(if ((actor.image ?: "").startsWith("http")) actor.image ?: "" else "")
			}

			AlertDialog(
				onDismissRequest = onDismiss,
				title = { Text(stringResource(Res.string.label_edit_actor)) },
				text = {
					Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
						TextField(
							value = name,
							onValueChange = { name = it },
							label = { Text(stringResource(Res.string.label_name)) },
							modifier = Modifier.fillMaxWidth()
						)
						TextField(
							value = url,
							onValueChange = { url = it },
							label = { Text(stringResource(Res.string.label_image_url)) },
							modifier = Modifier.fillMaxWidth()
						)
						if (url.isNotBlank()) {
							AsyncImage(
								model = ImageRequest.Builder(context)
									.data(url)
									.size(Size.ORIGINAL)
									.crossfade(true)
									.build(),
								contentDescription = null,
								modifier = Modifier.fillMaxWidth().aspectRatio(1.77f)
							)
						}
					}
				},
				confirmButton = {
					Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
						Button(
							onClick = onDeduplicate,
							colors = ButtonDefaults.buttonColors(backgroundColor = Color.Black)
						) {
							Text(stringResource(Res.string.button_deduplicate), color = Color.LightGray)
						}
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
							onClick = { if (name.isNotBlank()) onConfirm(name, url) },
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
