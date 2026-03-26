package pl.chopeks.screenmodel

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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
import pl.chopeks.screenmodel.model.UiState
import pl.chopeks.usecase.video.GetVideosUseCase

class VideosScreenModel(
	private val videoPlayer: IVideoPlayer,
	private val videoRepository: IVideoRepository,
	private val actorRepository: IActorRepository,
	private val categoryRepository: ICategoryRepository,
	private val taskManager: ITaskManager,
	private val getVideosUseCase: GetVideosUseCase
) : ScreenModel {
	data class VideosPage(
		val items: List<Video> = emptyList(),
		val currentPage: Long = 0,
		val pageCount: Long = 0
	)

	data class ToolbarState(
		val actors: List<Actor> = emptyList(),
		val categories: List<Category> = emptyList(),
		val filters: FilterParams = FilterParams(),
	)

	data class FilterParams(
		val page: Long = 0,
		val selectedActors: List<Actor> = emptyList(),
		val selectedCategories: List<Category> = emptyList(),
		val filterType: Int = 0,
		val version: Int = 0
	)

	private var actorLookup = emptyMap<Int, Actor>()
	private var categoryLookup = emptyMap<Int, Category>()

	private val localVideoUpdate = MutableStateFlow<Map<Int, Video>>(emptyMap())

	private val _actors = MutableStateFlow<List<Actor>?>(null)
	private val _categories = MutableStateFlow<List<Category>?>(null)

	private val _editingVideo = MutableStateFlow<Video?>(null)
	val editingVideo = _editingVideo.asStateFlow()

	private val _filterState = MutableStateFlow(FilterParams())
	val filterState = _filterState.asStateFlow()

	private val lookupsReady = _actors.combine(_categories) { actors, categories ->
		if (actors == null || categories == null)
			return@combine null
		actorLookup = actors.associateBy { it.id }
		categoryLookup = categories.associateBy { it.id }
		actors to categories
	}.filterNotNull()

	val toolbarState: StateFlow<UiState<ToolbarState>> = lookupsReady
		.combine(filterState) { lookups, filter ->
			UiState.Success(ToolbarState(lookups.first, lookups.second, filter))
		}
		.stateIn(
			scope = screenModelScope,
			started = SharingStarted.WhileSubscribed(5000),
			initialValue = UiState.Loading
		)

	val uiState: StateFlow<UiState<VideosPage>> = toolbarState
		.filterIsInstance<UiState.Success<ToolbarState>>()
		.map { it.data.filters }
		.flatMapLatest { filter ->
			flow {
				emit(UiState.Loading)
				val page = with(getVideosUseCase({ actorLookup[it] }, { categoryLookup[it] }) {
					page = filter.page
					selectedActors = filter.selectedActors
					selectedCategories = filter.selectedCategories
					filterType = filter.filterType
				}) {
					VideosPage(
						items = videos,
						currentPage = filter.page,
						pageCount = (count - 1) / pageSize
					)
				}
				emit(UiState.Success(page))
			}
		}
		.catch { emit(UiState.Error(it.message ?: "Unknown Error")) }
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

	fun init(actor: Actor?, category: Category?) {
		screenModelScope.launch {
			_actors.emit(actorRepository.getActors())
			_categories.emit(categoryRepository.getCategories())
			_filterState.update { current ->
				current.copy(
					selectedActors = actor?.let(::listOf) ?: emptyList(),
					selectedCategories = category?.let(::listOf) ?: emptyList()
				)
			}
		}
	}

	fun changePage(page: Int) {
		val state = uiState.value
		localVideoUpdate.value = emptyMap()
		_filterState.update { current ->
			val maxPage = if (state is UiState.Success) state.data.pageCount else 0
			current.copy(page = (current.page + page).coerceIn(0, maxPage), version = current.version + 1)
		}
	}

	fun toggleSortFilter() {
		_filterState.update { current ->
			current.copy(filterType = (current.filterType + 1) % 2, page = 0)
		}
	}

	fun play(video: Video) {
		screenModelScope.launch {
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
		screenModelScope.launch {
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
		screenModelScope.launch {
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
		_filterState.update { current ->
			val newSelection = if (current.selectedActors.any { it.id == actor.id }) {
				current.selectedActors.filter { it.id != actor.id }
			} else {
				current.selectedActors + actor
			}
			current.copy(selectedActors = newSelection, page = 0)
		}
	}

	fun toggleSelection(category: Category) {
		_filterState.update { current ->
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
		_filterState.update { it.copy(version = it.version + 1) }
	}

	private suspend fun updateEditingVideo(video: Video) {
		with(videoRepository.getInfo(video)) {
			val updatedVideo = video.copy(
				chips = VideoChips(
					actors.mapNotNull(actorLookup::get),
					categories.mapNotNull(categoryLookup::get)
				)
			)
			_editingVideo.emit(updatedVideo)
			if (editingVideo.value != null) {
				localVideoUpdate.update { it + (updatedVideo.id to updatedVideo) }
			}
		}
	}
}