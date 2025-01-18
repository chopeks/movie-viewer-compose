package pl.chopeks.movies.internal.screenmodel

import androidx.compose.runtime.mutableStateListOf
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.launch
import pl.chopeks.movies.bestConcurrencyDispatcher
import pl.chopeks.movies.internal.webservice.ActorsAPI
import pl.chopeks.movies.model.Actor

class ActorsScreenModel(
  private val webService: ActorsAPI
): ScreenModel {

  val actors = mutableStateListOf<Actor>()

  fun getActors() {
    screenModelScope.launch(bestConcurrencyDispatcher()) {
      val data = webService.getActors()
      actors.clear()
      actors.addAll(data)
      for (actor in data) {
        screenModelScope.launch(bestConcurrencyDispatcher()) {
          actors[actors.indexOf(actor)] = actor.copy(image = webService.getImage(actor))
        }
      }
    }
  }

  override fun onDispose() {
    webService.close()
    super.onDispose()
  }
}