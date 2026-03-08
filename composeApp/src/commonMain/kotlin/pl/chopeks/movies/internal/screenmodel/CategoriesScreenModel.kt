package pl.chopeks.movies.internal.screenmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.toLowerCase
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pl.chopeks.core.IImageConverter
import pl.chopeks.core.data.repository.ICategoryRepository
import pl.chopeks.core.model.Actor
import pl.chopeks.movies.bestConcurrencyDispatcher
import pl.chopeks.core.model.Category
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class CategoriesScreenModel(
  private val repository: ICategoryRepository,
  private val imageConverter: IImageConverter
): ScreenModel {
  var searchFilter by mutableStateOf("")
  val categories = MutableStateFlow<List<Category>>(emptyList())
  val filteredCategories: StateFlow<List<Category>> = snapshotFlow { searchFilter }
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
      with(repository.getCategories().sortedBy { it.name.toLowerCase(Locale.current) }) {
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

  @OptIn(ExperimentalEncodingApi::class)
  private fun updateCategoryImage(id: Int, image: String?) {
    val decoded = image?.let { Base64.Mime.decode(it) }
    categories.update { currentList ->
      currentList.map { category ->
        if (category.id == id) category.copy(image = image, imageBytes = decoded) else category
      }
    }
  }

  override fun onDispose() {
    repository.close()
    super.onDispose()
  }
}