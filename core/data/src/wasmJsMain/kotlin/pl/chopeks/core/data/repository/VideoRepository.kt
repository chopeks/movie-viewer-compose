package pl.chopeks.core.data.repository

import pl.chopeks.core.data.utils.RpcWrapper
import pl.chopeks.core.model.*

class VideoRepository(
	private val delegate: IVideoRepository
) : IVideoRepository, RpcWrapper {
	override suspend fun getVideos(from: Long, actors: List<Actor>, categories: List<Category>, filter: Int, count: Int): VideoContainer = rpc {
		delegate.getVideos(from, actors, categories, filter, count)
	}

	override suspend fun getVideoPath(video: Video): String? = rpc {
		delegate.getVideoPath(video)
	}

	override suspend fun getImage(video: Video): String? = rpc {
		delegate.getImage(video)
	}

	override suspend fun refreshImage(video: Video): String? = rpc {
		delegate.refreshImage(video)
	}

	override suspend fun getInfo(video: Video): VideoInfo = rpc {
		delegate.getInfo(video)
	}

	override suspend fun remove(video: Video) = rpc {
		delegate.remove(video)
	}

	override suspend fun moveToDump(video: Video) = rpc {
		delegate.moveToDump(video)
	}
}