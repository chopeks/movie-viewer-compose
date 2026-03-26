package pl.chopeks.movies.screen

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import pl.chopeks.core.data.bestConcurrencyDispatcher
import pl.chopeks.core.ffmpeg.FfmpegManager
import pl.chopeks.core.fpcalc.FpcalcManager
import pl.chopeks.screenmodel.model.UiState

class SettingsPlatformScreenModel(
	private val ffmpegManager: FfmpegManager,
	private val fpcalcManager: FpcalcManager,
) : ScreenModel {
	data class SettingsPage(
		val fpcalcStatus: Boolean? = null,
		val pythonStatus: Boolean? = null,
	)

	val uiState = MutableStateFlow<UiState<SettingsPage>>(UiState.Loading)

	init {
		screenModelScope.launch(bestConcurrencyDispatcher()) {
			uiState.emit(
				UiState.Success(
					SettingsPage(
						fpcalcStatus = fpcalcManager.isFpcalcAvailable()
					)
				)
			)
		}
	}

	fun ffcalcTest() = launchWithState {
		uiState.emit(UiState.Success(it.copy(fpcalcStatus = fpcalcManager.isFpcalcAvailable())))
	}

	private fun launchWithState(block: suspend CoroutineScope.(SettingsPage) -> Unit): Job {
		return screenModelScope.launch(bestConcurrencyDispatcher()) {
			val state = uiState.value as? UiState.Success
				?: return@launch
			block(state.data)
		}
	}
}