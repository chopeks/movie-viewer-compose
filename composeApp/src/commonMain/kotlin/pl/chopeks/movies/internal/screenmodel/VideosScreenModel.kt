package pl.chopeks.movies.internal.screenmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import pl.chopeks.core.data.repository.IActorRepository
import pl.chopeks.movies.bestConcurrencyDispatcher
import pl.chopeks.movies.internal.webservice.CategoriesAPI
import pl.chopeks.movies.internal.webservice.VideosAPI
import pl.chopeks.core.model.Actor
import pl.chopeks.core.model.Category
import pl.chopeks.core.model.Video
import pl.chopeks.core.model.VideoChips
import kotlin.math.max
import kotlin.math.min

class VideosScreenModel(
  private val webService: VideosAPI,
  private val actorWebService: IActorRepository,
  private val categoryWebService: CategoriesAPI,
) : ScreenModel {
  private var isInitialized = false
  var isBusy = false
    private set

  val actors = mutableStateListOf<Actor>()
  val categories = mutableStateListOf<Category>()

  var currentPage by mutableStateOf(0L)
  var count by mutableStateOf(0L)
  var filter by mutableStateOf(0)

  val videos = mutableStateListOf<Video>()
  val selectedActors = mutableStateListOf<Actor>()
  val selectedCategories = mutableStateListOf<Category>()

  fun init() {
    screenModelScope.launch(bestConcurrencyDispatcher()) {
      actors.addAll(actorWebService.getActors())
      categories.addAll(categoryWebService.getCategories())
      isInitialized = true
      getVideos()
    }
  }

  fun changePage(page: Int) {
    if (isBusy)
      return
    if (videos.size == 0)
      return
    videos.clear()
    val newPage = min(count, max(0, currentPage + page))
    currentPage = newPage
    getVideos()
  }

  fun getVideos() {
    if (!isInitialized)
      return
    isBusy = true
    screenModelScope.launch(bestConcurrencyDispatcher()) {
      val data = webService.getVideos(currentPage, selectedActors, selectedCategories, filter)
      videos.clear()
      videos.addAll(data.movies)
      count = (data.count - 1) / 15
      for (video in data.movies) {
        screenModelScope.launch(bestConcurrencyDispatcher()) {
          val index = videos.indexOf(video)
          if (index < 0)
            return@launch
          val info = webService.getInfo(video)
          videos[index] = video.copy(
            image = webService.getImage(video),
            chips = VideoChips(
              info.actors.map { id -> actors.first { it.id == id } },
              info.categories.map { id -> categories.first { it.id == id } }
            )
          )
        }
      }
      delay(500)
      isBusy = false
    }
  }

  fun play(video: Video) {
    screenModelScope.launch(bestConcurrencyDispatcher()) {
      webService.play(video)
    }
  }

  fun toggle(video: Video, actor: Actor) {
    screenModelScope.launch(bestConcurrencyDispatcher()) {
      val index = videos.indexOf(video)
      if (video.chips?.actors?.any { it.id == actor.id } == true) {
        actorWebService.unbind(actor, video)
      } else {
        actorWebService.bind(actor, video)
      }
      val info = webService.getInfo(video)
      videos[index] = video.copy(chips = VideoChips(
        info.actors.map { id -> actors.first { it.id == id } },
        info.categories.map { id -> categories.first { it.id == id } }
      ))
    }
  }

  fun toggle(video: Video, category: Category) {
    screenModelScope.launch(bestConcurrencyDispatcher()) {
      val index = videos.indexOf(video)
      if (video.chips?.categories?.any { it.id == category.id } == true) {
        categoryWebService.unbind(category, video)
      } else {
        categoryWebService.bind(category, video)
      }
      val info = webService.getInfo(video)
      videos[index] = video.copy(chips = VideoChips(
        info.actors.map { id -> actors.first { it.id == id } },
        info.categories.map { id -> categories.first { it.id == id } }
      ))
    }
  }

  fun generateThumbnail(video: Video) {
    screenModelScope.launch(bestConcurrencyDispatcher()) {
      val index = videos.indexOf(video)
      videos[index] = video.copy(image = webService.refreshImage(video))
    }
  }

  fun remove(video: Video) {
    screenModelScope.launch(bestConcurrencyDispatcher()) {
      webService.remove(video)
      videos.clear()
      getVideos()
    }
  }

  override fun onDispose() {
    super.onDispose()
    webService.close()
    actorWebService.close()
    categoryWebService.close()
  }
}