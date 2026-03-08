package pl.chopeks.movies.internal.webservice

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import pl.chopeks.core.model.Duplicates
import pl.chopeks.core.model.DuplicatesCount

class DuplicatesAPI(
	private val httpClient: HttpClient
) : AutoCloseable {
	private suspend fun get(path: String) = httpClient.get(Backend.URL + path)
	private suspend fun delete(path: String) = httpClient.delete(Backend.URL + path) {}

	suspend fun get(): List<Duplicates> {
		return get("certain_duplicates").body<List<Duplicates>>()
	}

	suspend fun cancel(model: Duplicates) {
		delete("/duplicates/cancel/${model.list.first().id}/${model.list.last().id}").body<Any>()
	}

	override fun close() {
		httpClient.close()
	}

	suspend fun count(): Int {
		return get("/duplicates/left").body<DuplicatesCount>().count
	}
}