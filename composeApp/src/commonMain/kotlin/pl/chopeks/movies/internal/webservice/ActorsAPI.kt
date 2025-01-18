package pl.chopeks.movies.internal.webservice

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import pl.chopeks.movies.model.Actor
import pl.chopeks.movies.model.Video

class ActorsAPI(
  private val httpClient: HttpClient,
) : AutoCloseable {
  private suspend fun get(path: String) = httpClient.get(Backend.URL + path)
  private suspend fun post(path: String) = httpClient.post(Backend.URL + path) {}
  private suspend fun post(path: String, body: Any) = httpClient.post(Backend.URL + path) {
    contentType(ContentType.Application.Json)
    setBody(body)
  }
  private suspend fun delete(path: String) = httpClient.delete(Backend.URL + path) {}


  suspend fun getActors(): List<Actor> {
    return get("actors").body<List<Actor>>()
      .sortedBy { it.name.lowercase() }
  }

  suspend fun getImage(actor: Actor): String? {
    return get("image/actor/${actor.id}").body<Array<String>>().firstOrNull()
      ?.substringAfter(",")
  }

  suspend fun bind(actor: Actor, video: Video) {
    post("actors/${actor.id}/${video.id}").body<Any>()
  }

  suspend fun unbind(actor: Actor, video: Video) {
    delete("actors/${actor.id}/${video.id}").body<Any>()
  }

  override fun close() {
    httpClient.close()
  }
}