package pl.chopeks.movies.internal.screenmodel

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import pl.chopeks.core.IImageConverter
import pl.chopeks.core.data.repository.ICategoryRepository
import pl.chopeks.core.model.Category
import pl.chopeks.movies.bestConcurrencyDispatcher
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class CategoriesScreenModel(
  private val repository: ICategoryRepository,
  private val imageConverter: IImageConverter
): ScreenModel {
  private val _searchFilter = MutableStateFlow("")
  var searchFilter = _searchFilter.asStateFlow()

  val categories = MutableStateFlow<List<Category>>(emptyList())
  val filteredCategories: StateFlow<List<Category>> = searchFilter
    .combine(categories) { filter, list ->
      if (filter.isBlank())
        return@combine list
      return@combine list.filter { it.name.contains(filter, ignoreCase = true) }
    }
    .stateIn(
      scope = screenModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = emptyList()
    )

  fun getCategories() {
    screenModelScope.launch(bestConcurrencyDispatcher()) {
      with(repository.getCategories().sortedBy { it.name.lowercase() }) {
        categories.value = this
        forEach { actor ->
          launch { updateCategoryImage(actor.id, repository.getImage(actor)) }
        }
      }
    }
  }

  fun add(name: String, url: String) {
    screenModelScope.launch(bestConcurrencyDispatcher()) {
      repository.add(name, imageConverter.urlToBase64(url, 425, 240))
      getCategories()
    }
  }

  fun edit(category: Category, name: String, url: String) {
    screenModelScope.launch(bestConcurrencyDispatcher()) {
      repository.edit(category.id, name, imageConverter.urlToBase64(url, 425, 240))
      getCategories()
    }
  }

  fun remove(category: Category) {
    screenModelScope.launch(bestConcurrencyDispatcher()) {
      repository.delete(category)
      getCategories()
    }
  }

  fun updateSearchFilter(filter: String) {
    _searchFilter.value = filter
  }

  @OptIn(ExperimentalEncodingApi::class)
  private fun updateCategoryImage(id: Int, image: String?) {
    val decoded = image?.let { Base64.Mime.decode(it) }
    categories.update { currentList ->
      currentList.map { category ->
        if (category.id == id) category.copy(image = image, imageBytes = decoded) else category
      }
    }
  }
}