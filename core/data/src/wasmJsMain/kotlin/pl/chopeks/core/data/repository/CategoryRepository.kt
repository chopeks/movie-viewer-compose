package pl.chopeks.core.data.repository

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import pl.chopeks.core.data.Backend
import pl.chopeks.core.model.Category
import pl.chopeks.core.model.Video

class CategoryRepository(
	private val httpClient: HttpClient,
) : ICategoryRepository {
	private suspend fun get(path: String) = httpClient.get(Backend.URL + path)
	private suspend fun post(path: String) = httpClient.post(Backend.URL + path) {}
	private suspend fun post(path: String, body: Any) = httpClient.post(Backend.URL + path) {
		contentType(ContentType.Application.Json)
		setBody(body)
	}

	private suspend fun delete(path: String) = httpClient.delete(Backend.URL + path) {}

	override suspend fun getCategories(): List<Category> {
		return get("categories").body<List<Category>>()
			.sortedBy { it.name.lowercase() }
	}

	override suspend fun getImage(category: Category): String? {
		return get("/image/category/${category.id}").body<Array<String?>>().firstOrNull()
			?.substringAfter(",")
	}

	override suspend fun bind(category: Category, video: Video) {
		post("categories/${category.id}/${video.id}").body<Any>()
	}

	override suspend fun unbind(category: Category, video: Video) {
		delete("categories/${category.id}/${video.id}").body<Any>()
	}

	override suspend fun add(name: String, url: String) {
		post("category", mapOf("name" to name, "url" to url)).body<Any>()
	}

	override suspend fun edit(id: Int, name: String, url: String) {
		post("category", Category(id, name, url)).body<Any>()
	}

	override fun close() {
		httpClient.close()
	}
}