package pl.chopeks.core.data.repository

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import pl.chopeks.core.data.Backend
import pl.chopeks.core.model.Path
import pl.chopeks.core.model.Settings

class SettingsRepository(
	private val httpClient: HttpClient,
) : ISettingsRepository {
	private suspend fun get(path: String) = httpClient.get(Backend.URL + path)
	private suspend fun post(path: String, body: Any) = httpClient.post(Backend.URL + path) {
		contentType(ContentType.Application.Json)
		setBody(body)
	}

	private suspend fun delete(path: String) = httpClient.delete(Backend.URL + path) {}

	override suspend fun getSettings(): Settings {
		return get("settings").body<Settings>()
	}

	override suspend fun setSettings(settings: Settings) {
		post("settings", settings).body<Any>()
	}

	override suspend fun getPaths(): List<Path> {
		return get("directories").body<List<Path>>()
	}

	override suspend fun removePath(path: Path) {
		post("directory/remove", path).body<Any>()
	}

	override suspend fun addPath(path: String) {
		post("directory", Path(path, 0)).body<Any>()
	}

	override fun close() {
		httpClient.close()
	}

}