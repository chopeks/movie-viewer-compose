package pl.chopeks.movies.internal.screenmodel

import androidx.compose.runtime.mutableStateListOf
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import pl.chopeks.movies.bestConcurrencyDispatcher
import pl.chopeks.movies.internal.webservice.ActorsAPI
import pl.chopeks.movies.internal.webservice.CategoriesAPI
import pl.chopeks.movies.model.Actor
import pl.chopeks.movies.model.Category

class CategoriesScreenModel(
  private val webService: CategoriesAPI
): ScreenModel {

  val categories = mutableStateListOf<Category>()

  fun getCategories() {
    screenModelScope.launch(bestConcurrencyDispatcher()) {
      val data = webService.getCategories()
      categories.clear()
      categories.addAll(data)
      for (category in data) {
        screenModelScope.launch(bestConcurrencyDispatcher()) {
          categories[categories.indexOf(category)] = category.copy(image = webService.getImage(category))
        }
      }
    }
  }

  override fun onDispose() {
    webService.close()
    super.onDispose()
  }
}