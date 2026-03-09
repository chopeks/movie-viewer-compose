package pl.chopeks.movies.internal.webservice

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import pl.chopeks.core.model.Actor
import pl.chopeks.core.model.Category
import pl.chopeks.core.model.Video
import pl.chopeks.core.model.VideoContainer
import pl.chopeks.core.model.VideoInfo

class VideosAPI(
  private val httpClient: HttpClient,
) : AutoCloseable {
  private suspend fun get(path: String) = httpClient.get(Backend.URL + path)

  suspend fun refreshImage(video: Video): String? {
    return get("image/movie/${video.id}?refresh=true").body<Array<String?>>().firstOrNull()
      ?.substringAfter(",")
  }

  suspend fun play(video: Video) {
    get("movie/play/${video.id}").body<Any>()
  }

  override fun close() {
    httpClient.close()
  }
}