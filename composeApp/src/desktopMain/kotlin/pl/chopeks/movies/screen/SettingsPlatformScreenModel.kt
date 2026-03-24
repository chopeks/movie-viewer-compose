package pl.chopeks.movies.screen

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import pl.chopeks.core.UiState
import pl.chopeks.core.data.bestConcurrencyDispatcher
import pl.chopeks.core.ffmpeg.FfmpegManager
import pl.chopeks.core.fpcalc.FpcalcManager

class SettingsPlatformScreenModel(
	private val ffmpegManager: FfmpegManager,
	private val fpcalcManager: FpcalcManager,
) : ScreenModel {
	data class SettingsPage(
		val ffmpegStatus: Boolean? = null,
		val ffprobeStatus: Boolean? = null,
		val fpcalcStatus: Boolean? = null,
		val pythonStatus: Boolean? = null,
	)

	val uiState = MutableStateFlow<UiState<SettingsPage>>(UiState.Loading)

	init {
		screenModelScope.launch(bestConcurrencyDispatcher()) {
			uiState.emit(
				UiState.Success(
					SettingsPage(
						ffmpegStatus = ffmpegManager.isFfmpegAvailable(),
						ffprobeStatus = ffmpegManager.isFfprobeAvailable(),
						fpcalcStatus = fpcalcManager.isFpcalcAvailable()
					)
				)
			)
		}
	}

	fun ffmpegTest() = launchWithState {
		uiState.emit(UiState.Success(it.copy(ffmpegStatus = ffmpegManager.isFfmpegAvailable())))
	}

	fun ffprobeTest() = launchWithState {
		uiState.emit(UiState.Success(it.copy(ffmpegStatus = ffmpegManager.isFfprobeAvailable())))
	}

	fun ffcalcTest() = launchWithState {
		uiState.emit(UiState.Success(it.copy(ffprobeStatus = fpcalcManager.isFpcalcAvailable())))
	}

	private fun launchWithState(block: suspend CoroutineScope.(SettingsPage) -> Unit): Job {
		return screenModelScope.launch(bestConcurrencyDispatcher()) {
			val state = uiState.value as? UiState.Success
				?: return@launch
			block(state.data)
		}
	}
}