package pl.chopeks.screenmodel

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import pl.chopeks.core.data.IImageConverter
import pl.chopeks.core.data.bestConcurrencyDispatcher
import pl.chopeks.core.data.repository.IActorRepository
import pl.chopeks.core.data.repository.IDuplicateRepository
import pl.chopeks.core.model.Actor
import pl.chopeks.core.model.IntRect
import pl.chopeks.core.utils.runIf
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class ActorsScreenModel(
	private val repository: IActorRepository,
	private val duplicatesRepository: IDuplicateRepository,
	private val imageConverter: IImageConverter,
	private val dispatcher: CoroutineDispatcher = bestConcurrencyDispatcher()
) : ScreenModel {
	sealed class Intent {
		object LoadActors : Intent()
		data class UpdateSearch(val query: String) : Intent()
		data class AddActor(val name: String, val imageBytes: ByteArray? = null, val rect: IntRect) : Intent()
		data class EditActor(val actor: Actor, val name: String, val imageBytes: ByteArray? = null, val rect: IntRect) : Intent()
		data class RemoveActor(val actor: Actor) : Intent()
		data class Deduplicate(val actor: Actor) : Intent()
	}

	data class UiState(
		val isLoading: Boolean = false,
		val actors: List<Actor> = emptyList(),
		val searchFilter: String = ""
	)

	private val _rawActors = MutableStateFlow<List<Actor>>(emptyList())
	private val _searchQuery = MutableStateFlow("")
	private val _isLoading = MutableStateFlow(false)

	val filteredActors: Flow<List<Actor>> =
		combine(_rawActors, _searchQuery) { actors, query ->
			actors.runIf(query.isNotBlank()) {
				filter { it.name.contains(query, ignoreCase = true) }
			}
		}.flowOn(dispatcher)

	val state: StateFlow<UiState> = combine(
		filteredActors,
		_searchQuery,
		_isLoading
	) { actors, query, loading ->
		UiState(
			actors = actors.runIf(query.isNotBlank()) {
				filter { it.name.contains(query, ignoreCase = true) }
			},
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
			is Intent.LoadActors -> load()
			is Intent.UpdateSearch -> _searchQuery.value = intent.query
			is Intent.AddActor -> add(intent.name, intent.imageBytes, intent.rect)
			is Intent.EditActor -> edit(intent.actor, intent.name, intent.imageBytes, intent.rect)
			is Intent.RemoveActor -> remove(intent.actor)
			is Intent.Deduplicate -> deduplicate(intent.actor)
		}
	}

	private fun load() {
		screenModelScope.launch {
			_isLoading.value = true
			val fetched = repository.getActors().sortedBy { it.name.lowercase() }
			_rawActors.value = fetched
			_isLoading.value = false

			// Background Image Loading
			fetched.forEach { actor ->
				launch { fetchImage(actor) }
			}
		}
	}

	private fun add(name: String, imageBytes: ByteArray?, rect: IntRect) {
		screenModelScope.launch {
			val image = imageBytes?.let { imageConverter.bytesToBase64(it, 269, 384, rect) }
			repository.add(name, image)
			load()
		}
	}

	private fun edit(actor: Actor, name: String, imageBytes: ByteArray?, rect: IntRect) {
		screenModelScope.launch {
			val image = imageBytes?.let { imageConverter.bytesToBase64(it, 269, 384, rect) }
			repository.edit(actor.id, name, image)
			load()
		}
	}

	private fun remove(actor: Actor) {
		screenModelScope.launch {
			repository.delete(actor)
			load()
		}
	}

	private fun deduplicate(actor: Actor) {
		screenModelScope.launch {
			duplicatesRepository.deduplicate(actor)
		}
	}

	@OptIn(ExperimentalEncodingApi::class)
	private suspend fun fetchImage(actor: Actor) {
		repository.getImage(actor)?.let { img ->
			val decoded = Base64.Mime.decode(img)
			_rawActors.update { current ->
				current.map {
					if (it.id == actor.id) it.copy(image = img, imageBytes = decoded) else it
				}
			}
		}
	}
}
