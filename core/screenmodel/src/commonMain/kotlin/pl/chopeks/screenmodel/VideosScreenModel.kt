package pl.chopeks.screenmodel

import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import pl.chopeks.screenmodel.model.UiEffect
import pl.chopeks.usecase.video.GetVideosUseCase

class VideosScreenModel(
	private val videoPlayer: IVideoPlayer,
	private val videoRepository: IVideoRepository,
	private val actorRepository: IActorRepository,
	private val categoryRepository: ICategoryRepository,
	private val taskManager: ITaskManager,
	private val getVideosUseCase: GetVideosUseCase
) : BaseScreenModel() {

	sealed class Intent {
		data class Init(val actor: Actor?, val category: Category?) : Intent()
		data class ChangePage(val page: Int) : Intent()
		data object ToggleSortFilter : Intent()
		data class Play(val video: Video) : Intent()
		data class PlayVideoAtIndex(val index: Int) : Intent()
		data class SetEditing(val video: Video?) : Intent()
		data class ToggleActorBinding(val video: Video, val actor: Actor) : Intent()
		data class ToggleCategoryBinding(val video: Video, val category: Category) : Intent()
		data class ToggleActorSelection(val actor: Actor) : Intent()
		data class ToggleCategorySelection(val category: Category) : Intent()
		data class GenerateThumbnail(val video: Video) : Intent()
		data class Remove(val video: Video) : Intent()
		data class Dump(val video: Video) : Intent()
		data object RefreshData : Intent()
	}

	data class VideosPage(
		val items: List<Video> = emptyList(),
		val currentPage: Long = 0,
		val pageCount: Long = 0
	)

	data class FilterParams(
		val page: Long = 0,
		val selectedActors: List<Actor> = emptyList(),
		val selectedCategories: List<Category> = emptyList(),
		val filterType: Int = 0,
		val version: Int = 0
	)

	data class UiState(
		val isLoading: Boolean = false,
		val videosPage: VideosPage = VideosPage(),
		val actors: List<Actor> = emptyList(),
		val categories: List<Category> = emptyList(),
		val filters: FilterParams = FilterParams(),
		val editingVideo: Video? = null,
		val error: String? = null
	)

	private val _state = MutableStateFlow(UiState())
	val state: StateFlow<UiState> = _state.asStateFlow()

	private val localVideoUpdate = MutableStateFlow<Map<Int, Video>>(emptyMap())
	private var actorLookup = emptyMap<Int, Actor>()
	private var categoryLookup = emptyMap<Int, Category>()

	init {
		screenModelScope.launch {
			val actors = actorRepository.getActors()
			val categories = categoryRepository.getCategories()
			actorLookup = actors.associateBy { it.id }
			categoryLookup = categories.associateBy { it.id }
			_state.update { it.copy(actors = actors, categories = categories) }
		}

		observeVideos()
	}

	@OptIn(ExperimentalCoroutinesApi::class)
	private fun observeVideos() {
		_state.map { it.filters }
			.distinctUntilChanged()
			.flatMapLatest { filter ->
				flow {
					emit(Result.Loading)
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
					emit(Result.Success(page))
				}.catch { emit(Result.Error(it.message ?: "Unknown Error")) }
			}
			.onEach { result ->
				_state.update {
					when (result) {
						is Result.Loading -> it.copy(isLoading = true)
						is Result.Success -> it.copy(isLoading = false, videosPage = result.data, error = null)
						is Result.Error -> it.copy(isLoading = false, error = result.message)
					}
				}
			}
			.launchIn(screenModelScope)

		localVideoUpdate.onEach { updates ->
			_state.update { state ->
				if (updates.isNotEmpty()) {
					val patchedItems = state.videosPage.items.map { video ->
						updates[video.id] ?: video
					}
					state.copy(videosPage = state.videosPage.copy(items = patchedItems))
				} else {
					state
				}
			}
		}.launchIn(screenModelScope)
	}

	fun handleIntent(intent: Intent) {
		when (intent) {
			is Intent.Init -> init(intent.actor, intent.category)
			is Intent.ChangePage -> changePage(intent.page)
			is Intent.ToggleSortFilter -> toggleSortFilter()
			is Intent.Play -> play(intent.video)
			is Intent.PlayVideoAtIndex -> playVideoAtIndex(intent.index)
			is Intent.SetEditing -> setEditing(intent.video)
			is Intent.ToggleActorBinding -> toggleBinding(intent.video, intent.actor)
			is Intent.ToggleCategoryBinding -> toggleBinding(intent.video, intent.category)
			is Intent.ToggleActorSelection -> toggleSelection(intent.actor)
			is Intent.ToggleCategorySelection -> toggleSelection(intent.category)
			is Intent.GenerateThumbnail -> generateThumbnail(intent.video)
			is Intent.Remove -> remove(intent.video)
			is Intent.Dump -> dump(intent.video)
			is Intent.RefreshData -> refreshData()
		}
	}

	private fun init(actor: Actor?, category: Category?) {
		_state.update { current ->
			current.copy(
				filters = current.filters.copy(
					selectedActors = actor?.let(::listOf) ?: emptyList(),
					selectedCategories = category?.let(::listOf) ?: emptyList()
				)
			)
		}
	}

	private fun changePage(page: Int) {
		localVideoUpdate.value = emptyMap()
		_state.update { current ->
			val maxPage = current.videosPage.pageCount
			current.copy(filters = current.filters.copy(page = (current.filters.page + page).coerceIn(0, maxPage), version = current.filters.version + 1))
		}
	}

	private fun toggleSortFilter() {
		_state.update { current ->
			current.copy(filters = current.filters.copy(filterType = (current.filters.filterType + 1) % 2, page = 0))
		}
	}

	private fun play(video: Video) {
		launchSafe {
			videoPlayer.play(video)
		}
	}

	private fun playVideoAtIndex(index: Int) {
		val items = _state.value.videosPage.items
		if (index in items.indices) {
			play(items[index])
		}
	}

	private fun setEditing(video: Video?) {
		_state.update { it.copy(editingVideo = video) }
	}

	private fun toggleBinding(video: Video, actor: Actor) {
		launchSafe {
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

	private fun toggleBinding(video: Video, category: Category) {
		launchSafe {
			val isBound = video.chips?.categories?.any { it.id == category.id } == true
			if (isBound) {
				categoryRepository.unbind(category, video)
			} else {
				categoryRepository.bind(category, video)
			}
			updateEditingVideo(video)
		}
	}

	private fun toggleSelection(actor: Actor) {
		_state.update { current ->
			val newSelection = if (current.filters.selectedActors.any { it.id == actor.id }) {
				current.filters.selectedActors.filter { it.id != actor.id }
			} else {
				current.filters.selectedActors + actor
			}
			current.copy(filters = current.filters.copy(selectedActors = newSelection, page = 0))
		}
	}

	private fun toggleSelection(category: Category) {
		_state.update { current ->
			val newSelection = if (current.filters.selectedCategories.any { it.id == category.id }) {
				current.filters.selectedCategories.filter { it.id != category.id }
			} else {
				current.filters.selectedCategories + category
			}
			current.copy(filters = current.filters.copy(selectedCategories = newSelection, page = 0))
		}
	}

	private fun generateThumbnail(video: Video) {
		screenModelScope.launch(bestConcurrencyDispatcher()) {
			val newVideo = video.copy(image = videoRepository.refreshImage(video))
			localVideoUpdate.update { it + (newVideo.id to newVideo) }
		}
	}

	private fun remove(video: Video) {
		screenModelScope.launch(bestConcurrencyDispatcher()) {
			videoRepository.remove(video)
			refreshData()
		}
	}

	private fun dump(video: Video) {
		screenModelScope.launch(bestConcurrencyDispatcher()) {
			videoRepository.moveToDump(video)
			refreshData()
		}
	}

	private fun refreshData() {
		_state.update { it.copy(filters = it.filters.copy(version = it.filters.version + 1)) }
	}

	private suspend fun updateEditingVideo(video: Video) {
		with(videoRepository.getInfo(video)) {
			val updatedVideo = video.copy(
				chips = VideoChips(
					actors.mapNotNull(actorLookup::get),
					categories.mapNotNull(categoryLookup::get)
				)
			)
			_state.update { it.copy(editingVideo = updatedVideo) }
			localVideoUpdate.update { it + (updatedVideo.id to updatedVideo) }
		}
	}

	override suspend fun emitEffect(throwable: Throwable) {
		emitEffect(UiEffect.Toast(throwable.message ?: "Unknown Error"))
	}

	private sealed class Result<out T> {
		data object Loading : Result<Nothing>()
		data class Success<T>(val data: T) : Result<T>()
		data class Error(val message: String) : Result<Nothing>()
	}
}
