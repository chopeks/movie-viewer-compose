package pl.chopeks.core.data.repository

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import pl.chopeks.core.IVideoPlayer
import pl.chopeks.core.data.Backend
import pl.chopeks.core.model.*

class VideoRepository(
	private val httpClient: HttpClient,
) : IVideoRepository, IVideoPlayer {
	private suspend fun get(path: String) = httpClient.get(Backend.URL + path)
	private suspend fun delete(path: String) = httpClient.delete(Backend.URL + path) {}

	override suspend fun getVideos(from: Long, actors: List<Actor>, categories: List<Category>, filter: Int, count: Int): VideoContainer {
		var path = "movie/${from * 15}/15?"
		path += if (categories.isNotEmpty())
			"&category=${categories.joinToString(",") { it.id.toString() }}"
		else
			"&category=null"
		path += if (actors.isNotEmpty())
			"&actor=${actors.joinToString(",") { it.id.toString() }}"
		else
			"&actor=null"

		path += "&filter=$filter"
		return get(path).body<VideoContainer>()
	}

	@Deprecated("wasm not supported")
	override suspend fun getVideoPath(video: Video): String? {
		return null
	}

	override suspend fun getImage(video: Video): String? {
		return get("image/movie/${video.id}").body<Array<String?>>().firstOrNull()
			?.substringAfter(",")
	}

	override suspend fun refreshImage(video: Video): String? {
		return get("image/movie/${video.id}?refresh=true").body<Array<String?>>().firstOrNull()
			?.substringAfter(",")
	}

	override suspend fun getInfo(video: Video): VideoInfo {
		return get("movie/${video.id}").body()
	}

	override suspend fun play(video: Video) {
		get("movie/play/${video.id}").body<Any>()
	}

	override suspend fun remove(video: Video) {
		delete("movie/${video.id}").body<Any>()
	}

	override fun close() {
		httpClient.close()
	}
}