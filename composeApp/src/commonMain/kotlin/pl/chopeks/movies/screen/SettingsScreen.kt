package pl.chopeks.movies.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.kodein.rememberScreenModel
import kotlinx.coroutines.launch
import movieviewer.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import pl.chopeks.movies.composables.ScreenSkeleton
import pl.chopeks.movies.composables.SettingsDirectory
import pl.chopeks.movies.composables.SettingsHeaderText
import pl.chopeks.movies.composables.state.AlertDialogState
import pl.chopeks.movies.composables.state.rememberAlertDialogState
import pl.chopeks.movies.internal.screenmodel.SettingsScreenModel

class SettingsScreen : Screen {
	@Composable
	override fun Content() {
		val scope = rememberCoroutineScope()
		val screenModel = rememberScreenModel<SettingsScreenModel>()
		val addDialog = rememberAlertDialogState()

		ScreenSkeleton(
			title = stringResource(Res.string.screen_settings)
		) { scope ->
			Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(2.dp)) {
				SettingsHeaderText(stringResource(Res.string.label_directories))
				Column {
					screenModel.pathes.forEach { path ->
						SettingsDirectory(path) {
							screenModel.removePath(it)
						}
					}
				}
				Button({ scope.launch { addDialog.show() } }) { Text(stringResource(Res.string.button_add)) }
				SettingsHeaderText(stringResource(Res.string.label_settings))
				if (screenModel.settings != null) {
					var browser by remember { mutableStateOf(screenModel.settings!!.browser) }
					var moviePlayer by remember { mutableStateOf(screenModel.settings!!.moviePlayer) }
					TextField(browser, { browser = it }, label = { Text(stringResource(Res.string.label_browser)) })
					TextField(moviePlayer, { moviePlayer = it }, label = { Text(stringResource(Res.string.label_player)) })
					Button({
						screenModel.saveSettings(browser, moviePlayer)
					}) { Text(stringResource(Res.string.button_save)) }
				}
				AddDialog(addDialog, screenModel)

				LaunchedEffect(screenModel) {
					screenModel.init()
				}
			}
		}
	}

	@Composable
	fun AddDialog(dialogState: AlertDialogState, screenModel: SettingsScreenModel) {
		if (dialogState.isVisible) {
			val scope = rememberCoroutineScope()
			var path by remember { mutableStateOf("") }
			AlertDialog(
				onDismissRequest = { scope.launch { dialogState.hide() } },
				title = { Text(stringResource(Res.string.button_add_directory)) },
				text = {
					TextField(path, { path = it }, label = { Text(stringResource(Res.string.label_path)) }, modifier = Modifier.fillMaxWidth())
				},
				confirmButton = {
					Button(onClick = {
						if (path.isNotBlank()) {
							screenModel.addPath(path)
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
}