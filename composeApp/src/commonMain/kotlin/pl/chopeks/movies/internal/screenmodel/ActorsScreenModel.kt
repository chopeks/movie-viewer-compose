package pl.chopeks.movies.internal.screenmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.launch
import pl.chopeks.movies.bestConcurrencyDispatcher
import pl.chopeks.movies.internal.webservice.ActorsAPI
import pl.chopeks.movies.model.Actor

class ActorsScreenModel(
  private val webService: ActorsAPI
): ScreenModel {
  var searchFilter by mutableStateOf("")

  val actors = mutableStateListOf<Actor>()
  val filteredActors = mutableStateListOf<Actor>()

  fun getActors() {
    screenModelScope.launch(bestConcurrencyDispatcher()) {
      val data = webService.getActors()
      actors.clear()
      actors.addAll(data)
      for (actor in data) {
        screenModelScope.launch(bestConcurrencyDispatcher()) {
          actors[actors.indexOf(actor)] = actor.copy(image = webService.getImage(actor))
          filter()
        }
      }
    }
  }

  fun add(name: String, url: String) {
    screenModelScope.launch(bestConcurrencyDispatcher()) {
      webService.add(name, url)
      getActors()
    }
  }

  fun edit(actor: Actor, name: String, url: String) {
    screenModelScope.launch(bestConcurrencyDispatcher()) {
      webService.edit(actor.id, name, url)
      getActors()
    }
  }

  fun filter() {
    if (actors.isEmpty())
      return
    screenModelScope.launch(bestConcurrencyDispatcher()) {
      filteredActors.clear()
      filteredActors.addAll(actors.filter { searchFilter.lowercase() in it.name.lowercase() })
    }
  }

  override fun onDispose() {
    webService.close()
    super.onDispose()
  }
}