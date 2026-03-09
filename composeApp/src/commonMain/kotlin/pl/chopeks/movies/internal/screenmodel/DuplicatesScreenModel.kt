package pl.chopeks.movies.internal.screenmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import pl.chopeks.core.IVideoPlayer
import pl.chopeks.core.data.repository.IDuplicateRepository
import pl.chopeks.core.data.repository.IVideoRepository
import pl.chopeks.core.model.Duplicates
import pl.chopeks.core.model.Video
import pl.chopeks.movies.bestConcurrencyDispatcher

class DuplicatesScreenModel(
	private val videoPlayer: IVideoPlayer,
	private val videoRepository: IVideoRepository,
	private val duplicatesRepository: IDuplicateRepository,
) : ScreenModel {
	var count by mutableStateOf(0)
	val duplicates = mutableStateListOf<Duplicates>()

	init {
		screenModelScope.launch(bestConcurrencyDispatcher()) {
			while (isActive) {
				count = duplicatesRepository.count()
				delay(10000)
			}
		}
	}

	fun getDuplicates() {
		screenModelScope.launch(bestConcurrencyDispatcher()) {
			duplicates.clear()
			val entries = duplicatesRepository.getCertainDuplicates().toMutableList()
			for (duplicate in entries) {
				val index = entries.indexOf(duplicate)
				val videos = duplicate.list
				entries[index] = duplicate.copy(
					list = listOf(
						videos.first().copy(image = videoRepository.getImage(videos.first())),
						videos.last().copy(image = videoRepository.getImage(videos.last())),
					)
				)
			}
			duplicates.clear()
			duplicates.addAll(entries)
		}
	}

	fun cancel(model: Duplicates) {
		screenModelScope.launch(bestConcurrencyDispatcher()) {
			duplicatesRepository.cancel(model)
			duplicates.clear()
			getDuplicates()
		}
	}

	fun remove(model: Video) {
		screenModelScope.launch(bestConcurrencyDispatcher()) {
			videoRepository.remove(model)
			duplicates.clear()
			getDuplicates()
		}
	}

	fun play(video: Video) {
		screenModelScope.launch(bestConcurrencyDispatcher()) {
			videoPlayer.play(video)
		}
	}

	override fun onDispose() {
		super.onDispose()
		videoPlayer.close()
		videoRepository.close()
		duplicatesRepository.close()
	}
}