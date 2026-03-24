package pl.chopeks.screenmodel

import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import pl.chopeks.core.data.IVideoPlayer
import pl.chopeks.core.data.bestConcurrencyDispatcher
import pl.chopeks.core.data.repository.IDuplicateRepository
import pl.chopeks.core.data.repository.IVideoRepository
import pl.chopeks.core.model.Duplicates
import pl.chopeks.core.model.Video
import pl.chopeks.screenmodel.model.UiEffect
import pl.chopeks.screenmodel.model.UiState
import pl.chopeks.usecase.video.GetDuplicatesUseCase

class DuplicatesScreenModel(
	private val videoPlayer: IVideoPlayer,
	private val videoRepository: IVideoRepository,
	private val duplicatesRepository: IDuplicateRepository,
	private val getDuplicatesUseCase: GetDuplicatesUseCase,
) : BaseScreenModel() {
	private val updateTrigger = MutableStateFlow(0)

	private val _count = MutableStateFlow(0)
	val count = _count.asStateFlow()

	val duplicates = updateTrigger
		.buffer(capacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
		.transform {
			emit(UiState.Loading)
			emit(UiState.Success(getDuplicatesUseCase()))
		}
		.catch { e -> emit(UiState.Error(e.message ?: "Unknown Error")) }
		.flowOn(bestConcurrencyDispatcher())
		.stateIn(
			scope = screenModelScope,
			started = SharingStarted.WhileSubscribed(5000),
			initialValue = UiState.Loading
		)

	init {
		screenModelScope.launch(bestConcurrencyDispatcher()) {
			while (isActive) {
				_count.value = duplicatesRepository.count()
				if ((duplicates.value !is UiState.Success) || (duplicates.value as UiState.Success).data.isEmpty()) {
					updateTrigger.value++
				}
				delay(5000)
			}
		}
	}

	fun cancel(model: Duplicates) {
		launchAndUpdate {
			duplicatesRepository.cancel(model)
		}
	}

	fun remove(model: Video) {
		launchAndUpdate {
			videoRepository.remove(model)
		}
	}

	fun dump(video: Video) {
		launchAndUpdate {
			videoRepository.moveToDump(video)
		}
	}

	fun play(video: Video) {
		launchSafe {
			videoPlayer.play(video)
		}
	}

	fun deduplicate() {
		launchSafe {
			duplicatesRepository.deduplicateAll()
		}
	}

	private fun launchAndUpdate(block: suspend () -> Unit) = launchSafe {
		block(); updateTrigger.value++
	}

	override suspend fun emitEffect(throwable: Throwable) = when (throwable) {
		else -> emitEffect(UiEffect.Toast(throwable.message ?: "Unknown Error"))
	}
}