package pl.chopeks.screenmodel

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import pl.chopeks.core.data.repository.IEncoderRepository
import pl.chopeks.core.data.service.IVideoEncodingService
import pl.chopeks.core.model.EncodeStatus
import pl.chopeks.screenmodel.model.UiState

class EncoderScreenModel(
	private val encodingService: IVideoEncodingService,
	private val encoderRepository: IEncoderRepository
): ScreenModel {
	data class Page(
		val encoder: Map<String, EncodeStatus> = mapOf()
	)

	val uiState = MutableStateFlow<UiState<Page>>(UiState.Loading)

	init {
		screenModelScope.launch {
			uiState.emit(
				UiState.Success(
					Page(
						encoder = emptyMap()
					)
				)
			)
			encoderRepository.observeEncodingStatus().collect {
				launchWithState { state ->
					uiState.emit(UiState.Success(state.copy(encoder = it)))
				}
			}
		}
	}

	fun startEncoder() = screenModelScope.launch {
		encodingService.startQueue()
	}

	private fun launchWithState(block: suspend CoroutineScope.(Page) -> Unit): Job {
		return screenModelScope.launch {
			val state = uiState.value as? UiState.Success
				?: return@launch
			block(state.data)
		}
	}
}