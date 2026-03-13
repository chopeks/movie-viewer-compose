package pl.chopeks.movies.internal.screenmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.toLowerCase
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import pl.chopeks.core.IImageConverter
import pl.chopeks.core.data.repository.IActorRepository
import pl.chopeks.core.data.repository.IDuplicateRepository
import pl.chopeks.core.model.Actor
import pl.chopeks.movies.bestConcurrencyDispatcher
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class ActorsScreenModel(
	private val repository: IActorRepository,
	private val duplicatesRepository: IDuplicateRepository,
	private val imageConverter: IImageConverter
) : ScreenModel {
	var searchFilter by mutableStateOf("")
	val actors = MutableStateFlow<List<Actor>>(emptyList())
	val filteredActors: StateFlow<List<Actor>> = snapshotFlow { searchFilter }
		.combine(actors) { filter, list ->
			if (filter.isBlank())
				return@combine list
			return@combine list.filter { it.name.contains(filter, ignoreCase = true) }
		}
		.stateIn(
			scope = screenModelScope,
			started = SharingStarted.WhileSubscribed(5000),
			initialValue = emptyList()
		)

	fun getActors() {
		screenModelScope.launch(bestConcurrencyDispatcher()) {
			with(repository.getActors().sortedBy { it.name.toLowerCase(Locale.current) }) {
				actors.value = this
				forEach { actor ->
					launch { updateActorImage(actor.id, repository.getImage(actor)) }
				}
			}
		}
	}

	fun add(name: String, url: String) {
		screenModelScope.launch(bestConcurrencyDispatcher()) {
			repository.add(name, imageConverter.urlToBase64(url, 269, 384))
			getActors()
		}
	}

	fun edit(actor: Actor, name: String, url: String) {
		screenModelScope.launch(bestConcurrencyDispatcher()) {
			repository.edit(actor.id, name, imageConverter.urlToBase64(url, 269, 384))
			getActors()
		}
	}

	fun remove(actor: Actor) {
		screenModelScope.launch(bestConcurrencyDispatcher()) {
			repository.delete(actor)
			getActors()
		}
	}

	fun dedup(actor: Actor) {
		screenModelScope.launch(bestConcurrencyDispatcher()) {
			duplicatesRepository.deduplicate(actor)
		}
	}

	@OptIn(ExperimentalEncodingApi::class)
	private fun updateActorImage(id: Int, image: String?) {
		val decoded = image?.let { Base64.Mime.decode(it) }
		actors.update { currentList ->
			currentList.map { actor ->
				if (actor.id == id) actor.copy(image = image, imageBytes = decoded) else actor
			}
		}
	}
}