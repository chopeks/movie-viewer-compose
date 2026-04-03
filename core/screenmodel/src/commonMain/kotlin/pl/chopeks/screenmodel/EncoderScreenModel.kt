package pl.chopeks.screenmodel

import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import pl.chopeks.core.data.repository.IEncoderRepository
import pl.chopeks.core.model.EncodeStatus
import pl.chopeks.core.model.priority
import pl.chopeks.screenmodel.model.UiEffect

class EncoderScreenModel(
	private val encoderRepository: IEncoderRepository
) : BaseScreenModel() {

	sealed class Intent {
	}

	data class UiState(
		val isLoading: Boolean = false,
		val encoder: Map<String, EncodeStatus> = emptyMap(),
		val error: String? = null
	)

	private val _state = MutableStateFlow(UiState())
	val state: StateFlow<UiState> = _state.asStateFlow()

	init {
		screenModelScope.launch {
			_state.update { it.copy(isLoading = true) }
			encoderRepository.observeEncodingStatus()
				.onEach { status ->
					_state.update {
						it.copy(
							isLoading = false,
							encoder = status.entries
								.sortedBy { it.value.priority() }
								.associate { it.key to it.value }
						)
					}
				}
				.catch { e ->
					_state.update { it.copy(isLoading = false, error = e.message ?: "Unknown Error") }
				}
				.launchIn(screenModelScope)
		}
	}

	fun handleIntent(intent: Intent) {
		when (intent) {
			else -> {}
		}
	}

	override suspend fun emitEffect(throwable: Throwable) {
		emitEffect(UiEffect.Toast(throwable.message ?: "Unknown Error"))
	}
}
