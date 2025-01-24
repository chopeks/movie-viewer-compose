package pl.chopeks.movies.internal.webservice

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import pl.chopeks.movies.model.*

class SettingsAPI(
  private val httpClient: HttpClient,
) : AutoCloseable {
  private suspend fun get(path: String) = httpClient.get(Backend.URL + path)
  private suspend fun post(path: String, body: Any) = httpClient.post(Backend.URL + path) {
    contentType(ContentType.Application.Json)
    setBody(body)
  }
  private suspend fun delete(path: String) = httpClient.delete(Backend.URL + path) {}

  suspend fun get(): Settings {
    return get("settings").body<Settings>()
  }

  suspend fun set(settings: Settings) {
    post("settings", settings).body<Any>()
  }

  suspend fun getPathes(): List<Path> {
    return get("directories").body<List<Path>>()
  }

  suspend fun remove(path: Path) {
    post("directory/remove", path).body<Any>()
  }

  suspend fun add(path: String) {
    post("directory", Path(path, 0)).body<Any>()
  }

  override fun close() {
    httpClient.close()
  }

}