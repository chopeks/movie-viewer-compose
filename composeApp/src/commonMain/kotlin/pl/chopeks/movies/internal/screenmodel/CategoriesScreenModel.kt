package pl.chopeks.movies.internal.screenmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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

  var searchFilter by mutableStateOf("")
  val categories = mutableStateListOf<Category>()
  val filteredCategories = mutableStateListOf<Category>()

  fun getCategories() {
    screenModelScope.launch(bestConcurrencyDispatcher()) {
      val data = webService.getCategories()
      categories.clear()
      categories.addAll(data)
      for (category in data) {
        screenModelScope.launch(bestConcurrencyDispatcher()) {
          categories[categories.indexOf(category)] = category.copy(image = webService.getImage(category))
          filter()
        }
      }
    }
  }

  fun add(name: String, url: String) {
    screenModelScope.launch(bestConcurrencyDispatcher()) {
      webService.add(name, url)
      getCategories()
    }
  }

  fun edit(category: Category, name: String, url: String) {
    screenModelScope.launch(bestConcurrencyDispatcher()) {
      webService.edit(category.id, name, url)
      getCategories()
    }
  }

  fun filter() {
    if (categories.isEmpty())
      return
    screenModelScope.launch(bestConcurrencyDispatcher()) {
      filteredCategories.clear()
      filteredCategories.addAll(categories.filter { searchFilter.lowercase() in it.name.lowercase() })
    }
  }

  override fun onDispose() {
    webService.close()
    super.onDispose()
  }
}