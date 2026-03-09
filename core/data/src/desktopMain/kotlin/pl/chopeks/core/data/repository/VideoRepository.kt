package pl.chopeks.core.data.repository

import pl.chopeks.core.database.datasource.VideoLocalDataSource
import pl.chopeks.core.model.*

class VideoRepository(
	private val dataSource: VideoLocalDataSource,
) : IVideoRepository {
	override suspend fun getVideos(from: Long, actors: List<Actor>, categories: List<Category>, filter: Int, count: Int): VideoContainer {
		return dataSource.getVideos(from, actors, categories, filter, count)
	}

	override suspend fun getImage(video: Video): String? {
		return dataSource.getImage(video)
	}

	override suspend fun refreshImage(video: Video): String? {
		return dataSource.getImage(video)
	}

	override suspend fun getInfo(video: Video): VideoInfo {
		return dataSource.getInfo(video)
	}

	override suspend fun remove(video: Video) {
		dataSource.remove(video)
	}

	override fun close() {
	}
}