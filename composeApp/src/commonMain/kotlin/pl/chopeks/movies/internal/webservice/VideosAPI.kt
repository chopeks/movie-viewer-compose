package pl.chopeks.movies.internal.webservice

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import pl.chopeks.movies.model.*

class VideosAPI(
  private val httpClient: HttpClient,
) : AutoCloseable {
  private suspend fun get(path: String) = httpClient.get(Backend.URL + path)
  private suspend fun delete(path: String) = httpClient.delete(Backend.URL + path) {}

  suspend fun getVideos(from: Long, actors: List<Actor>, categories: List<Category>, filter: Int): VideoContainer {
    var path = "movie/${from * 15}/15?"
    path += if (categories.isNotEmpty())
      "&category=${categories.joinToString(",") {it.id.toString()}}"
    else
      "&category=null"
    path += if (actors.isNotEmpty())
      "&actor=${actors.joinToString(",") {it.id.toString()}}"
    else
      "&actor=null"

    path += "&filter=$filter"
    return get(path).body<VideoContainer>()
  }

  suspend fun getImage(video: Video): String? {
    return get("/image/movie/${video.id}").body<Array<String>>().firstOrNull()
      ?.substringAfter(",")
  }

  suspend fun refreshImage(video: Video): String? {
    return get("/image/movie/${video.id}?refresh=true").body<Array<String>>().firstOrNull()
      ?.substringAfter(",")
  }

  suspend fun getInfo(video: Video): VideoInfo {
    return get("/movie/${video.id}").body()
  }

  suspend fun play(video: Video) {
    get("/movie/play/${video.id}").body<Any>()
  }

  suspend fun remove(video: Video) {
    delete("/movie/${video.id}").body<Any>()
  }

  override fun close() {
    httpClient.close()
  }
}