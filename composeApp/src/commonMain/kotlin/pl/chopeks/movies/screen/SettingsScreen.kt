package pl.chopeks.movies.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
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
import pl.chopeks.movies.composables.state.rememberAlertDialogState
import pl.chopeks.movies.utils.KeyEventManager
import pl.chopeks.movies.utils.KeyEventNavigation
import pl.chopeks.screenmodel.SettingsScreenModel

@Composable
expect fun ColumnScope.ExternalAppsContainer(screen: Screen)

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
			Column(
				modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
				verticalArrangement = Arrangement.spacedBy(2.dp)
			) {
				SettingsHeaderText(stringResource(Res.string.label_directories))
				Column {
					val paths by screenModel.paths.collectAsState()
					paths.forEach { path ->
						SettingsDirectory(path) {
							screenModel.removePath(it)
						}
					}
				}
				Button(onClick = { addDialogState.show() }) {
					Text(stringResource(Res.string.button_add))
				}
				SettingsHeaderText(stringResource(Res.string.label_settings))
				val settings by screenModel.settings.collectAsState()
				settings?.let { currentSettings ->
					var browser by remember(currentSettings) { mutableStateOf(currentSettings.browser) }
					var moviePlayer by remember(currentSettings) { mutableStateOf(currentSettings.moviePlayer) }
					var encoderSource by remember(currentSettings) { mutableStateOf(currentSettings.encoderSource) }
					var encoderSink by remember(currentSettings) { mutableStateOf(currentSettings.encoderSink) }

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

				ExternalAppsContainer(this@SettingsScreen)

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
