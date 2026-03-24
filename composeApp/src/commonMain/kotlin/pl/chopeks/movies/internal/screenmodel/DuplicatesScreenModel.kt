package pl.chopeks.movies.internal.screenmodel

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import pl.chopeks.core.UiState
import pl.chopeks.core.data.repository.IDuplicateRepository
import pl.chopeks.core.data.repository.IVideoRepository
import pl.chopeks.core.model.Duplicates
import pl.chopeks.core.model.Video
import pl.chopeks.movies.IVideoPlayer
import pl.chopeks.movies.bestConcurrencyDispatcher

class DuplicatesScreenModel(
	private val videoPlayer: IVideoPlayer,
	private val videoRepository: IVideoRepository,
	private val duplicatesRepository: IDuplicateRepository,
) : ScreenModel {
	private val updateTrigger = MutableStateFlow(0)

	private val _count = MutableStateFlow(0)
	val count = _count.asStateFlow()

	val duplicates = updateTrigger
		.buffer(capacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
		.transform {
			emit(UiState.Loading)
			val result = withContext(bestConcurrencyDispatcher()) {
				duplicatesRepository.getCertainDuplicates().map { duplicate ->
					async {
						val videos = duplicate.list
						duplicate.copy(
							list = listOf(
								videos.first().copy(image = videoRepository.getImage(videos.first())),
								videos.last().copy(image = videoRepository.getImage(videos.last()))
							)
						)
					}
				}.awaitAll()
			}
			emit(UiState.Success(result))
		}
		.flowOn(bestConcurrencyDispatcher())
		.catch { e -> emit(UiState.Error(e.message ?: "Unknown Error")) }
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
		screenModelScope.launchAndUpdate {
			duplicatesRepository.cancel(model)
		}
	}

	fun remove(model: Video) {
		screenModelScope.launchAndUpdate {
			videoRepository.remove(model)
		}
	}

	fun dump(video: Video) {
		screenModelScope.launchAndUpdate {
			videoRepository.moveToDump(video)
		}
	}

	fun play(video: Video) {
		screenModelScope.launch(bestConcurrencyDispatcher()) {
			videoPlayer.play(video)
		}
	}

	fun deduplicate() {
		screenModelScope.launch(bestConcurrencyDispatcher()) {
			duplicatesRepository.deduplicateAll()
		}
	}

	private fun CoroutineScope.launchAndUpdate(block: suspend () -> Unit) = launch(bestConcurrencyDispatcher()) {
		block()
		updateTrigger.value++
	}
}