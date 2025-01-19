package pl.chopeks.movies.internal.screenmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import pl.chopeks.movies.bestConcurrencyDispatcher
import pl.chopeks.movies.internal.webservice.ActorsAPI
import pl.chopeks.movies.internal.webservice.CategoriesAPI
import pl.chopeks.movies.internal.webservice.VideosAPI
import pl.chopeks.movies.model.Actor
import pl.chopeks.movies.model.Category
import pl.chopeks.movies.model.Video
import pl.chopeks.movies.model.VideoChips
import kotlin.math.max
import kotlin.math.min

class VideosScreenModel(
  private val webService: VideosAPI,
  private val actorWebService: ActorsAPI,
  private val categoryWebService: CategoriesAPI,
) : ScreenModel {
  private var isInitialized = false

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
    if (videos.size == 0)
      return
    videos.clear()
    currentPage = min(count, max(0, currentPage + page))
    getVideos()
  }

  fun getVideos() {
    if (!isInitialized)
      return
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
    }
  }

  fun play(video: Video) {
    screenModelScope.launch(bestConcurrencyDispatcher()) {
      webService.play(video)
    }
  }

  override fun onDispose() {
    super.onDispose()
    webService.close()
    actorWebService.close()
    categoryWebService.close()
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
}