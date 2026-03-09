package pl.chopeks.core.data.repository

import pl.chopeks.core.model.*

interface IVideoRepository: AutoCloseable {
	suspend fun getVideos(from: Long, actors: List<Actor>, categories: List<Category>, filter: Int, count: Int): VideoContainer
	suspend fun getImage(video: Video): String?
	suspend fun refreshImage(video: Video): String?
	suspend fun getInfo(video: Video): VideoInfo
	suspend fun remove(video: Video)
}