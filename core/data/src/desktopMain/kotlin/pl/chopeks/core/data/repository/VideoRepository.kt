package pl.chopeks.core.data.repository

import pl.chopeks.core.IImageConverter
import pl.chopeks.core.database.datasource.VideoLocalDataSource
import pl.chopeks.core.model.*

class VideoRepository(
	private val dataSource: VideoLocalDataSource,
	private val imageConverter: IImageConverter
) : IVideoRepository {
	override suspend fun getVideos(from: Long, actors: List<Actor>, categories: List<Category>, filter: Int, count: Int): VideoContainer {
		return dataSource.getVideos(from, actors, categories, filter, count)
	}

	override suspend fun getVideoPath(video: Video): String? {
		return dataSource.getVideoPath(video.id)
	}

	override suspend fun getImage(video: Video): String? {
		return dataSource.getImage(video)?.substringAfter(",")
	}

	override suspend fun refreshImage(video: Video): String? {
		val path = dataSource.getVideoPath(video.id)
			?: return getImage(video)
		val img = imageConverter.makeBase64Screenshot(path, (1..999).random().toLong())
		return dataSource.setImage(video, img)?.substringAfter(",")
	}

	override suspend fun getInfo(video: Video): VideoInfo {
		return dataSource.getInfo(video)
	}

	override suspend fun remove(video: Video) {
		dataSource.remove(video)
	}

	override suspend fun moveToDump(video: Video) {
		dataSource.moveToDump(video)
	}
}