package pl.chopeks.movies.internal.webservice

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import pl.chopeks.movies.model.Actor
import pl.chopeks.movies.model.Category
import pl.chopeks.movies.model.Video

class CategoriesAPI(
  private val httpClient: HttpClient,
) : AutoCloseable {
  private suspend fun get(path: String) = httpClient.get(Backend.URL + path)
  private suspend fun post(path: String) = httpClient.post(Backend.URL + path) {}
  private suspend fun post(path: String, body: Any) = httpClient.post(Backend.URL + path) {
    contentType(ContentType.Application.Json)
    setBody(body)
  }
  private suspend fun delete(path: String) = httpClient.delete(Backend.URL + path) {}

  suspend fun getCategories(): List<Category> {
    return get("categories").body<List<Category>>()
      .sortedBy { it.name.lowercase() }
  }

  suspend fun getImage(category: Category): String? {
    return get("/image/category/${category.id}").body<Array<String?>>().firstOrNull()
      ?.substringAfter(",")
  }

  suspend fun bind(category: Category, video: Video) {
    post("categories/${category.id}/${video.id}").body<Any>()
  }

  suspend fun unbind(category: Category, video: Video) {
    delete("categories/${category.id}/${video.id}").body<Any>()
  }

  override fun close() {
    httpClient.close()
  }
}