package pl.chopeks.movies.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
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
import pl.chopeks.movies.composables.ScreenSkeleton
import pl.chopeks.movies.composables.SettingsDirectory
import pl.chopeks.movies.composables.SettingsHeaderText
import pl.chopeks.movies.composables.StyledTextField
import pl.chopeks.movies.composables.cards.SettingsCapabilityCard
import pl.chopeks.movies.composables.cards.SettingsExternalSoftwareCard
import pl.chopeks.movies.composables.state.rememberAlertDialogState
import pl.chopeks.movies.utils.KeyEventManager
import pl.chopeks.movies.utils.KeyEventNavigation
import pl.chopeks.screenmodel.SettingsScreenModel

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
			screenModel.init()
		}

		ScreenSkeleton(
			title = stringResource(Res.string.screen_settings)
		) {
			val apps by screenModel.externalApps.collectAsState()
			val capabilities by screenModel.capabilities.collectAsState()
			val settings by screenModel.settings.collectAsState()
			val paths by screenModel.paths.collectAsState()

			LazyVerticalGrid(
				columns = GridCells.Adaptive(minSize = 500.dp),
				contentPadding = PaddingValues(16.dp),
				verticalArrangement = Arrangement.spacedBy(8.dp),
				horizontalArrangement = Arrangement.spacedBy(8.dp),
				modifier = Modifier.fillMaxWidth(),
			) {
				item(span = { GridItemSpan(maxLineSpan) }) {
					SettingsHeaderText(stringResource(Res.string.label_directories))
				}
				items(paths) { path ->
					SettingsDirectory(path) {
						screenModel.removePath(it)
					}
				}
				item(span = { GridItemSpan(maxLineSpan) }) {
					Button(onClick = { addDialogState.show() }) {
						Text(stringResource(Res.string.button_add))
					}
				}
				item(span = { GridItemSpan(maxLineSpan) }) {
					SettingsHeaderText(stringResource(Res.string.label_settings))
				}
				item {
					settings?.let { currentSettings ->
						var browser by remember(currentSettings) { mutableStateOf(currentSettings.browser) }
						var moviePlayer by remember(currentSettings) { mutableStateOf(currentSettings.moviePlayer) }
						var encoderSource by remember(currentSettings) { mutableStateOf(currentSettings.encoderSource) }
						var encoderSink by remember(currentSettings) { mutableStateOf(currentSettings.encoderSink) }

						Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
							StyledTextField(
								value = browser,
								onValueChange = { browser = it },
								label = stringResource(Res.string.label_browser),
								modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth()
							)
							StyledTextField(
								value = moviePlayer,
								onValueChange = { moviePlayer = it },
								label = stringResource(Res.string.label_player),
								modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth()
							)
							StyledTextField(
								value = encoderSource,
								onValueChange = { encoderSource = it },
								label = stringResource(Res.string.label_encoder_source),
								modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth()
							)
							StyledTextField(
								value = encoderSink,
								onValueChange = { encoderSink = it },
								label = stringResource(Res.string.label_encoder_sink),
								modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth()
							)

							Button(onClick = {
								screenModel.saveSettings(browser, moviePlayer, encoderSource, encoderSink)
							}) {
								Text(stringResource(Res.string.button_save))
							}
						}
					}
				}

				item(span = { GridItemSpan(maxLineSpan) }) {
					Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
						SettingsHeaderText("System Engines")
						IconButton(onClick = { screenModel.refreshApps() }) { Icon(Icons.Default.Refresh, contentDescription = "refresh") }
					}
				}
				items(apps.entries.toList(), { it.key }) { item ->
					SettingsExternalSoftwareCard(item.key, item.value.version)
				}
				item(span = { GridItemSpan(maxLineSpan) }) {
					Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
						SettingsHeaderText("Available Features")
						IconButton(onClick = { screenModel.refreshApps() }) { Icon(Icons.Default.Refresh, contentDescription = "refresh") }
					}
				}
				items(capabilities.entries.toList(), { it.key }) { item ->
					SettingsCapabilityCard(item.key.name, "does sth", isAvailable = item.value)
				}

				item(span = { GridItemSpan(maxLineSpan) }) {
					SettingsHeaderText("")
				}
			}

			AddDialog(
				isVisible = addDialogState.isVisible,
				onDismiss = { addDialogState.hide() },
				onConfirm = { path ->
					screenModel.addPath(path)
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
