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
import pl.chopeks.movies.bestConcurrencyDispatcher
import pl.chopeks.movies.internal.webservice.DuplicatesAPI
import pl.chopeks.movies.internal.webservice.VideosAPI
import pl.chopeks.movies.model.Duplicates
import pl.chopeks.movies.model.Video

class DuplicatesScreenModel(
  private val webService: DuplicatesAPI,
  private val videoWebService: VideosAPI
) : ScreenModel {
  var count by mutableStateOf(0)
  val duplicates = mutableStateListOf<Duplicates>()

  init {
    screenModelScope.launch(bestConcurrencyDispatcher()) {
      while (isActive) {
        count = webService.count()
        delay(10000)
      }
    }
  }

  fun getDuplicates() {
    screenModelScope.launch(bestConcurrencyDispatcher()) {
      duplicates.clear()
      val entries = webService.get().toMutableList()
      for (duplicate in entries) {
        val index = entries.indexOf(duplicate)
        val videos = duplicate.list
        entries[index] = duplicate.copy(
          list = listOf(
            videos.first().copy(image = videoWebService.getImage(videos.first())),
            videos.last().copy(image = videoWebService.getImage(videos.last())),
          )
        )
      }
      duplicates.clear()
      duplicates.addAll(entries)
    }
  }

  fun cancel(model: Duplicates) {
    screenModelScope.launch(bestConcurrencyDispatcher()) {
      webService.cancel(model)
      duplicates.clear()
      getDuplicates()
    }
  }

  fun remove(model: Video) {
    screenModelScope.launch(bestConcurrencyDispatcher()) {
      videoWebService.remove(model)
      duplicates.clear()
      getDuplicates()
    }
  }

  fun play(video: Video) {
    screenModelScope.launch(bestConcurrencyDispatcher()) {
      videoWebService.play(video)
    }
  }

  override fun onDispose() {
    super.onDispose()
    webService.close()
    videoWebService.close()
  }
}