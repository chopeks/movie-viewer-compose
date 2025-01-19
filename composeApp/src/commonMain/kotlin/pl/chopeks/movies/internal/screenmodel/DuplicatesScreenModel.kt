package pl.chopeks.movies.internal.screenmodel

import androidx.compose.runtime.mutableStateListOf
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
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

  val duplicates = mutableStateListOf<Duplicates>()

  fun getDuplicates() {
    screenModelScope.launch(bestConcurrencyDispatcher()) {
      duplicates.clear()
      duplicates.addAll(webService.get())
      for (duplicate in duplicates) {
        val index = duplicates.indexOf(duplicate)
        val videos = duplicate.list
        duplicates[index] = duplicate.copy(
          list = listOf(
            videos.first().copy(image = videoWebService.getImage(videos.first())),
            videos.last().copy(image = videoWebService.getImage(videos.last())),
          )
        )
      }
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