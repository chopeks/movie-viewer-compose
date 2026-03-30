package pl.chopeks.screenmodel

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import pl.chopeks.core.data.IImageConverter
import pl.chopeks.core.data.bestConcurrencyDispatcher
import pl.chopeks.core.data.repository.ICategoryRepository
import pl.chopeks.core.model.Category
import pl.chopeks.core.utils.runIf
import kotlin.io.encoding.Base64

class CategoriesScreenModel(
	private val repository: ICategoryRepository,
	private val imageConverter: IImageConverter,
	private val dispatcher: CoroutineDispatcher = bestConcurrencyDispatcher()
) : ScreenModel {
	sealed class Intent {
		object LoadCategories : Intent()
		data class UpdateSearch(val query: String) : Intent()
		data class AddCategory(val name: String, val url: String, val imageBytes: ByteArray? = null) : Intent()
		data class EditCategory(val category: Category, val name: String, val url: String, val imageBytes: ByteArray? = null) : Intent()
		data class RemoveCategory(val category: Category) : Intent()
	}

	data class UiState(
		val isLoading: Boolean = false,
		val categories: List<Category> = emptyList(),
		val searchFilter: String = ""
	)

	private val _rawCategories = MutableStateFlow<List<Category>>(emptyList())
	private val _searchQuery = MutableStateFlow("")
	private val _isLoading = MutableStateFlow(false)

	val filteredCategories: Flow<List<Category>> =
		combine(_rawCategories, _searchQuery) { categories, query ->
			categories.runIf(query.isNotBlank()) {
				filter { it.name.contains(query, ignoreCase = true) }
			}
		}.flowOn(dispatcher)

	val state: StateFlow<UiState> = combine(
		filteredCategories,
		_searchQuery,
		_isLoading
	) { categories, query, loading ->
		UiState(
			categories = categories,
			searchFilter = query,
			isLoading = loading
		)
	}.stateIn(
		scope = screenModelScope,
		started = SharingStarted.WhileSubscribed(5000),
		initialValue = UiState(isLoading = true)
	)

	fun handleIntent(intent: Intent) {
		when (intent) {
			is Intent.LoadCategories -> load()
			is Intent.UpdateSearch -> _searchQuery.value = intent.query
			is Intent.AddCategory -> add(intent.name, intent.url, intent.imageBytes)
			is Intent.EditCategory -> edit(intent.category, intent.name, intent.url, intent.imageBytes)
			is Intent.RemoveCategory -> remove(intent.category)
		}
	}

	private fun load() {
		screenModelScope.launch {
			_isLoading.value = true
			val fetched = repository.getCategories().sortedBy { it.name.lowercase() }
			_rawCategories.value = fetched
			_isLoading.value = false

			fetched.forEach { category ->
				launch { fetchImage(category) }
			}
		}
	}

	private fun add(name: String, url: String, imageBytes: ByteArray?) {
		screenModelScope.launch {
			val image = imageBytes?.let { imageConverter.bytesToBase64(it, 269, 384) }
			repository.add(name, image ?: imageConverter.urlToBase64(url, 269, 384))
			load()
		}
	}

	private fun edit(category: Category, name: String, url: String, imageBytes: ByteArray?) {
		screenModelScope.launch {
			val image = imageBytes?.let { imageConverter.bytesToBase64(it, 269, 384) }
			repository.edit(category.id, name, image ?: imageConverter.urlToBase64(url, 269, 384))
			load()
		}
	}

	private fun remove(category: Category) {
		screenModelScope.launch {
			repository.delete(category)
			load()
		}
	}

	private suspend fun fetchImage(category: Category) {
		repository.getImage(category)?.let { img ->
			val decoded = Base64.Mime.decode(img)
			_rawCategories.update { current ->
				current.map {
					if (it.id == category.id) it.copy(image = img, imageBytes = decoded) else it
				}
			}
		}
	}
}