package pl.chopeks.screenmodel

import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import pl.chopeks.core.data.IVideoPlayer
import pl.chopeks.core.data.bestConcurrencyDispatcher
import pl.chopeks.core.data.repository.IDuplicateRepository
import pl.chopeks.core.data.repository.IVideoRepository
import pl.chopeks.core.model.Duplicates
import pl.chopeks.core.model.Video
import pl.chopeks.screenmodel.model.UiEffect
import pl.chopeks.usecase.video.GetDuplicatesUseCase

class DuplicatesScreenModel(
	private val videoPlayer: IVideoPlayer,
	private val videoRepository: IVideoRepository,
	private val duplicatesRepository: IDuplicateRepository,
	private val getDuplicatesUseCase: GetDuplicatesUseCase,
) : BaseScreenModel() {

	sealed class Intent {
		data object Init : Intent()
		data class Cancel(val model: Duplicates) : Intent()
		data class Remove(val video: Video) : Intent()
		data class Dump(val video: Video) : Intent()
		data class Play(val video: Video) : Intent()
		data object DeduplicateAll : Intent()
	}

	data class UiState(
		val isLoading: Boolean = false,
		val duplicates: List<Duplicates> = emptyList(),
		val count: Int = 0,
		val error: String? = null
	)

	private val _state = MutableStateFlow(UiState())
	val state: StateFlow<UiState> = _state.asStateFlow()

	init {
		screenModelScope.launch(bestConcurrencyDispatcher()) {
			while (isActive) {
				val count = duplicatesRepository.count()
				_state.update { it.copy(count = count) }
				if (_state.value.duplicates.isEmpty()) {
					loadDuplicates()
				}
				delay(5000)
			}
		}
	}

	fun handleIntent(intent: Intent) {
		when (intent) {
			is Intent.Init -> loadDuplicates()
			is Intent.Cancel -> cancel(intent.model)
			is Intent.Remove -> remove(intent.video)
			is Intent.Dump -> dump(intent.video)
			is Intent.Play -> play(intent.video)
			is Intent.DeduplicateAll -> deduplicate()
		}
	}

	private fun loadDuplicates() {
		launchSafe {
			_state.update { it.copy(isLoading = true, error = null) }
			try {
				val result = getDuplicatesUseCase()
				_state.update { it.copy(isLoading = false, duplicates = result) }
			} catch (e: Exception) {
				_state.update { it.copy(isLoading = false, error = e.message ?: "Unknown Error") }
			}
		}
	}

	private fun cancel(model: Duplicates) {
		launchAndUpdate {
			duplicatesRepository.cancel(model)
		}
	}

	private fun remove(video: Video) {
		launchAndUpdate {
			videoRepository.remove(video)
		}
	}

	private fun dump(video: Video) {
		launchAndUpdate {
			videoRepository.moveToDump(video)
		}
	}

	private fun play(video: Video) {
		launchSafe {
			videoPlayer.play(video)
		}
	}

	private fun deduplicate() {
		launchSafe {
			duplicatesRepository.deduplicateAll()
		}
	}

	private fun launchAndUpdate(block: suspend () -> Unit) = launchSafe {
		block()
		loadDuplicates()
	}

	override suspend fun emitEffect(throwable: Throwable) = when (throwable) {
		else -> emitEffect(UiEffect.Toast(throwable.message ?: "Unknown Error"))
	}
}
