package pl.chopeks.movies.screen

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.kodein.rememberScreenModel
import movieviewer.composeapp.generated.resources.Res
import movieviewer.composeapp.generated.resources.button_test
import movieviewer.composeapp.generated.resources.label_external_apps
import org.jetbrains.compose.resources.stringResource
import pl.chopeks.core.UiState
import pl.chopeks.movies.composables.ProgressIndicator
import pl.chopeks.movies.composables.SettingsHeaderText
import pl.chopeks.movies.composables.buttons.SettingStateTestButton


@Composable
actual fun ColumnScope.ExternalAppsContainer(screen: Screen) {
	val screenModel = screen.rememberScreenModel<SettingsPlatformScreenModel>()
	val state by screenModel.uiState.collectAsState()

	when (val current = state) {
		is UiState.Loading -> ProgressIndicator()
		is UiState.Error -> Text("Error: ${current.message}")
		is UiState.Success -> {
			SettingsHeaderText(stringResource(Res.string.label_external_apps))
			SettingStateTestButton("ffmpeg", stringResource(Res.string.button_test), current.data.ffmpegStatus, onClick = { screenModel.ffmpegTest() })
			SettingStateTestButton("ffprobe", stringResource(Res.string.button_test), current.data.ffprobeStatus, onClick = { screenModel.ffprobeTest() })
			SettingStateTestButton("fpcalc", stringResource(Res.string.button_test), current.data.fpcalcStatus, onClick = { screenModel.ffcalcTest() })
		}
	}
}