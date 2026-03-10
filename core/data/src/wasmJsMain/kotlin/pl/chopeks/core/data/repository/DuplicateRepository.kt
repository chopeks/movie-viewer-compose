package pl.chopeks.core.data.repository

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import pl.chopeks.core.data.Backend
import pl.chopeks.core.model.Actor
import pl.chopeks.core.model.Duplicates
import pl.chopeks.core.model.DuplicatesCount

class DuplicateRepository(
	private val httpClient: HttpClient
) : IDuplicateRepository {
	private suspend fun get(path: String) = httpClient.get(Backend.URL + path)
	private suspend fun delete(path: String) = httpClient.delete(Backend.URL + path) {}

	override suspend fun getCertainDuplicates(): List<Duplicates> {
		return get("certain_duplicates").body<List<Duplicates>>()
	}

	override suspend fun cancel(model: Duplicates) {
		delete("/duplicates/cancel/${model.list.first().id}/${model.list.last().id}").body<Any>()
	}

	override suspend fun count(): Int {
		return get("/duplicates/left").body<DuplicatesCount>().count
	}

	override suspend fun deduplicate(actor: Actor) {
		get("/duplicates/dedup/actor/${actor.id}").body<Any>()
	}

	override suspend fun deduplicateAll() {
		get("/duplicates/dedup/all").body<Any>()
	}

	override fun close() {
		httpClient.close()
	}
}