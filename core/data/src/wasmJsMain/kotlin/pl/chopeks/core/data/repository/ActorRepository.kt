package pl.chopeks.core.data.repository

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import pl.chopeks.core.data.Backend
import pl.chopeks.core.model.Actor
import pl.chopeks.core.model.Video

class ActorRepository(
	private val httpClient: HttpClient,
) : IActorRepository {
	private suspend fun get(path: String) = httpClient.get(Backend.URL + path)
	private suspend fun post(path: String) = httpClient.post(Backend.URL + path) {}
	private suspend fun post(path: String, body: Any) = httpClient.post(Backend.URL + path) {
		contentType(ContentType.Application.Json)
		setBody(body)
	}

	private suspend fun delete(path: String) = httpClient.delete(Backend.URL + path) {}

	override suspend fun getActors(): List<Actor> {
		return get("actors").body<List<Actor>>()
			.sortedBy { it.name.lowercase() }
	}

	override suspend fun getActor(id: Int): Actor? {
		return get("actors/$id").body<Actor?>()
	}

	override suspend fun getImage(actor: Actor): String? {
		return get("image/actor/${actor.id}").body<Array<String?>>().firstOrNull()
			?.substringAfter(",")
	}

	override suspend fun bind(actor: Actor, video: Video) {
		post("actors/${actor.id}/${video.id}").body<Any>()
	}

	override suspend fun unbind(actor: Actor, video: Video) {
		delete("actors/${actor.id}/${video.id}").body<Any>()
	}

	override suspend fun add(name: String, url: String?) {
		post("actor", mapOf("name" to name, "url" to url)).body<Any>()
	}

	override suspend fun edit(id: Int, name: String, url: String?) {
		post("actor", Actor(id, name, url)).body<Any>()
	}

	override suspend fun delete(actor: Actor) {
		delete("actors/${actor.id}").body<Any>()
	}

	override fun close() {
		httpClient.close()
	}
}