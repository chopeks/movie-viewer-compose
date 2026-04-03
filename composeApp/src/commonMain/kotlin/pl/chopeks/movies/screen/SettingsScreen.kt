package pl.chopeks.movies.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.kodein.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import movieviewer.composeapp.generated.resources.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.kodein.di.compose.rememberInstance
import pl.chopeks.movies.composables.ScreenSkeleton
import pl.chopeks.movies.composables.SettingsDirectory
import pl.chopeks.movies.composables.SettingsHeaderText
import pl.chopeks.movies.composables.StyledTextField
import pl.chopeks.movies.composables.cards.SettingsCapabilityCard
import pl.chopeks.movies.composables.cards.SettingsExternalSoftwareCard
import pl.chopeks.movies.composables.state.rememberAlertDialogState
import pl.chopeks.movies.mapping.description
import pl.chopeks.movies.utils.KeyEventManager
import pl.chopeks.movies.utils.KeyEventNavigation
import pl.chopeks.screenmodel.SettingsScreenModel
import pl.chopeks.screenmodel.SettingsScreenModel.Intent.*

class SettingsScreen : Screen {
	@Composable
	override fun Content() {
		val screenModel = rememberScreenModel<SettingsScreenModel>()
		val keyEventManager by rememberInstance<KeyEventManager>()
		val navigator = LocalNavigator.current
		val addDialogState = rememberAlertDialogState()

		DisposableEffect(keyEventManager, navigator) {
			keyEventManager.setListener { KeyEventNavigation.onKeyEvent(it, navigator) }
			onDispose { keyEventManager.setListener(null) }
		}

		LaunchedEffect(screenModel) {
			screenModel.handleIntent(Init)
		}

		ScreenSkeleton(
			title = stringResource(Res.string.screen_settings),
			screenModel = screenModel
		) {
			val state by screenModel.state.collectAsState()

			val settings = state.settings

			LazyVerticalGrid(
				columns = GridCells.Adaptive(minSize = 500.dp),
				contentPadding = PaddingValues(16.dp),
				verticalArrangement = Arrangement.spacedBy(8.dp),
				horizontalArrangement = Arrangement.spacedBy(8.dp),
				modifier = Modifier.fillMaxWidth(),
			) {
				item(span = { GridItemSpan(maxLineSpan) }) {
					Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
						SettingsHeaderText(stringResource(Res.string.label_directories))
						IconButton(onClick = { addDialogState.show() }) {
							Icon(Icons.Default.Add, contentDescription = stringResource(Res.string.button_add))
						}
					}
				}
				items(state.paths) { path ->
					SettingsDirectory(path, onRemoveClick = {
						screenModel.handleIntent(RemovePath(it))
					})
				}

				if (settings != null) {
					item(span = { GridItemSpan(maxLineSpan) }) {
						Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
							SettingsHeaderText(stringResource(Res.string.label_settings))
							IconButton(onClick = {
								screenModel.handleIntent(SaveSettings)
							}) {
								Icon(
									painterResource(Res.drawable.content_save),
									contentDescription = stringResource(Res.string.button_save)
								)
							}
						}
					}

					item {
						StyledTextField(
							value = settings.browser,
							onValueChange = {
								screenModel.handleIntent(UpdateBrowser(it))
							},
							label = stringResource(Res.string.label_browser),
							modifier = Modifier.fillMaxWidth()
						)
					}
					item {
						StyledTextField(
							value = settings.moviePlayer,
							onValueChange = {
								screenModel.handleIntent(UpdateMoviePlayer(it))
							},
							label = stringResource(Res.string.label_player),
							modifier = Modifier.fillMaxWidth()
						)
					}
					item {
						StyledTextField(
							value = settings.encoderSource,
							onValueChange = {
								screenModel.handleIntent(UpdateEncoderSource(it))
							},
							label = stringResource(Res.string.label_encoder_source),
							modifier = Modifier.fillMaxWidth()
						)
					}
					item {
						StyledTextField(
							value = settings.encoderSink,
							onValueChange = {
								screenModel.handleIntent(UpdateEncoderSink(it))
							},
							label = stringResource(Res.string.label_encoder_sink),
							modifier = Modifier.fillMaxWidth()
						)
					}
				}

				item(span = { GridItemSpan(maxLineSpan) }) {
					Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
						SettingsHeaderText(stringResource(Res.string.label_external_apps))
						IconButton(onClick = { screenModel.handleIntent(RefreshApps) }) {
							Icon(
								Icons.Default.Refresh,
								contentDescription = "refresh"
							)
						}
					}
				}
				items(state.externalApps.entries.toList(), { it.key }) { item ->
					SettingsExternalSoftwareCard(item.key, item.value.version)
				}

				item(span = { GridItemSpan(maxLineSpan) }) {
					Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
						SettingsHeaderText(stringResource(Res.string.label_available_features))
						IconButton(onClick = { screenModel.handleIntent(RefreshApps) }) {
							Icon(
								Icons.Default.Refresh,
								contentDescription = "refresh"
							)
						}
					}
				}
				items(state.capabilities.entries.toList(), { it.key }) { item ->
					SettingsCapabilityCard(item.key.name, item.key.description(), isAvailable = item.value)
				}
			}

			AddDialog(
				isVisible = addDialogState.isVisible,
				onDismiss = { addDialogState.hide() },
				onConfirm = { path ->
					screenModel.handleIntent(AddPath(path))
					addDialogState.hide()
				}
			)
		}
	}

	@Composable
	private fun AddDialog(
		isVisible: Boolean,
		onDismiss: () -> Unit,
		onConfirm: (String) -> Unit
	) {
		if (isVisible) {
			var path by remember { mutableStateOf("") }
			AlertDialog(
				onDismissRequest = onDismiss,
				title = { Text(stringResource(Res.string.button_add_directory)) },
				text = {
					StyledTextField(
						value = path,
						onValueChange = { path = it },
						label = stringResource(Res.string.label_path),
						modifier = Modifier.fillMaxWidth()
					)
				},
				confirmButton = {
					Button(
						onClick = { if (path.isNotBlank()) onConfirm(path) },
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
}
