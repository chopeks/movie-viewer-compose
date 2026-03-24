package pl.chopeks.screenmodel

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import pl.chopeks.core.UiState
import pl.chopeks.core.data.ITaskManager
import pl.chopeks.core.data.IVideoPlayer
import pl.chopeks.core.data.bestConcurrencyDispatcher
import pl.chopeks.core.data.repository.IActorRepository
import pl.chopeks.core.data.repository.ICategoryRepository
import pl.chopeks.core.data.repository.IVideoRepository
import pl.chopeks.core.model.Actor
import pl.chopeks.core.model.Category
import pl.chopeks.core.model.Video
import pl.chopeks.core.model.VideoChips

class VideosScreenModel(
	private val videoPlayer: IVideoPlayer,
	private val videoRepository: IVideoRepository,
	private val actorRepository: IActorRepository,
	private val categoryRepository: ICategoryRepository,
	private val taskManager: ITaskManager
) : ScreenModel {
	data class VideosPage(
		val items: List<Video>,
		val currentPage: Long,
		val pageCount: Long
	)

	data class FilterParams(
		val initialized: Boolean = false,
		val page: Long = 0,
		val selectedActors: List<Actor> = emptyList(),
		val selectedCategories: List<Category> = emptyList(),
		val filterType: Int = 0,
		val version: Int = 0
	)

	private var actorLookup = emptyMap<Int, Actor>()
	private var categoryLookup = emptyMap<Int, Category>()

	private val localVideoUpdate = MutableStateFlow<Map<Int, Video>>(emptyMap())

	val filterState = MutableStateFlow(FilterParams())
	var uiState: StateFlow<UiState<VideosPage>> = filterState
		.flatMapLatest { params ->
			localVideoUpdate.value = emptyMap()
			flow {
				if (!params.initialized) {
					emit(UiState.Loading)
					return@flow
				}
				emit(UiState.Loading)
				try {
					val data = videoRepository.getVideos(params.page, params.selectedActors, params.selectedCategories, params.filterType, 15)
					val enriched = coroutineScope {
						data.movies.map { video ->
							async { enrichVideo(video) }
						}.awaitAll()
					}
					val page = VideosPage(
						items = enriched,
						currentPage = params.page,
						pageCount = (data.count - 1) / 15
					)
					emit(UiState.Success(page))
				} catch (e: Exception) {
					emit(UiState.Error(e.message ?: "Unknown Error"))
				}
			}
		}
		.flowOn(bestConcurrencyDispatcher())
		.combine(localVideoUpdate) { state, updates ->
			if (state is UiState.Success && updates.isNotEmpty()) {
				val patchedItems = state.data.items.map { video ->
					updates[video.id] ?: video
				}
				UiState.Success(state.data.copy(items = patchedItems))
			} else {
				state
			}
		}
		.stateIn(
			scope = screenModelScope,
			started = SharingStarted.WhileSubscribed(5000),
			initialValue = UiState.Loading
		)

	private val _actors = MutableStateFlow<List<Actor>>(emptyList())
	val actors = _actors.asStateFlow()


	private val _categories = MutableStateFlow<List<Category>>(emptyList())
	val categories = _categories.asStateFlow()

	private val _editingVideo = MutableStateFlow<Video?>(null)
	val editingVideo = _editingVideo.asStateFlow()

	fun init(actor: Actor?, category: Category?) {
		if (filterState.value.initialized)
			return
		screenModelScope.launch(bestConcurrencyDispatcher()) {
			val actors = actorRepository.getActors()
			actorLookup = actors.associateBy { it.id }
			_actors.emit(actors)

			val categories = categoryRepository.getCategories()
			categoryLookup = categories.associateBy { it.id }
			_categories.emit(categories)

			filterState.update { current ->
				current.copy(
					initialized = true,
					selectedActors = actor?.let(::listOf) ?: emptyList(),
					selectedCategories = category?.let(::listOf) ?: emptyList()
				)
			}
		}
	}

	fun changePage(page: Int) {
		val state = uiState.value
		filterState.update { current ->
			val maxPage = if (state is UiState.Success) state.data.pageCount else 0
			current.copy(page = (current.page + page).coerceIn(0, maxPage))
		}
	}

	fun toggleSortFilter() {
		filterState.update { current ->
			current.copy(filterType = (current.filterType + 1) % 2, page = 0)
		}
	}

	private suspend fun enrichVideo(video: Video): Video {
		val info = videoRepository.getInfo(video)
		return video.copy(
			image = videoRepository.getImage(video),
			chips = VideoChips(
				actors = info.actors.mapNotNull(transform = actorLookup::get),
				categories = info.categories.mapNotNull(transform = categoryLookup::get)
			)
		)
	}

	fun play(video: Video) {
		screenModelScope.launch(bestConcurrencyDispatcher()) {
			videoPlayer.play(video)
		}
	}

	fun playVideoAtIndex(index: Int) {
		val state = uiState.value
		if (state is UiState.Success) {
			val items = state.data.items
			if (index in items.indices) {
				play(items[index])
			}
		}
	}

	fun setEditing(video: Video?) {
		_editingVideo.value = video
	}

	fun toggleBinding(video: Video, actor: Actor) {
		screenModelScope.launch(bestConcurrencyDispatcher()) {
			val isBound = video.chips?.actors?.any { it.id == actor.id } == true
			if (isBound) {
				actorRepository.unbind(actor, video)
			} else {
				actorRepository.bind(actor, video)
				taskManager.startDedupTask()
			}
			updateEditingVideo(video)
		}
	}

	fun toggleBinding(video: Video, category: Category) {
		screenModelScope.launch(bestConcurrencyDispatcher()) {
			val isBound = video.chips?.categories?.any { it.id == category.id } == true
			if (isBound) {
				categoryRepository.unbind(category, video)
			} else {
				categoryRepository.bind(category, video)
			}
			updateEditingVideo(video)
		}
	}

	fun toggleSelection(actor: Actor) {
		filterState.update { current ->
			val newSelection = if (current.selectedActors.any { it.id == actor.id }) {
				current.selectedActors.filter { it.id != actor.id }
			} else {
				current.selectedActors + actor
			}
			current.copy(selectedActors = newSelection, page = 0)
		}
	}

	fun toggleSelection(category: Category) {
		filterState.update { current ->
			val newSelection = if (current.selectedCategories.any { it.id == category.id }) {
				current.selectedCategories.filter { it.id != category.id }
			} else {
				current.selectedCategories + category
			}
			current.copy(selectedCategories = newSelection, page = 0)
		}
	}

	fun generateThumbnail(video: Video) {
		screenModelScope.launch(bestConcurrencyDispatcher()) {
			val newVideo = video.copy(image = videoRepository.refreshImage(video))
			localVideoUpdate.update { it + (newVideo.id to newVideo) }
		}
	}

	fun remove(video: Video) {
		screenModelScope.launch(bestConcurrencyDispatcher()) {
			videoRepository.remove(video)
			refreshData()
		}
	}

	fun dump(video: Video) {
		screenModelScope.launch(bestConcurrencyDispatcher()) {
			videoRepository.moveToDump(video)
			refreshData()
		}
	}

	fun refreshData() {
		filterState.update { it.copy(version = it.version + 1) }
	}

	private suspend fun updateEditingVideo(video: Video) {
		with(videoRepository.getInfo(video)) {
			_editingVideo.value = video.copy(
				chips = VideoChips(
					actors.mapNotNull(actorLookup::get),
					categories.mapNotNull(categoryLookup::get)
				)
			)
			if (editingVideo.value != null) {
				localVideoUpdate.update { it + (editingVideo.value!!.id to editingVideo.value!!) }
			}
		}
	}
}