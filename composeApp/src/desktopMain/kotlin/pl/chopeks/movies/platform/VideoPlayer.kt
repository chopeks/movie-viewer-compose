package pl.chopeks.movies.platform

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pl.chopeks.core.IVideoPlayer
import pl.chopeks.core.data.repository.ISettingsRepository
import pl.chopeks.core.data.repository.IVideoRepository
import pl.chopeks.core.model.Video
import pl.chopeks.movies.server.utils.runCommand
import java.io.File

class VideoPlayer(
	private val repository: IVideoRepository,
	private val settingsRepository: ISettingsRepository,
): IVideoPlayer {
	override suspend fun play(video: Video) = withContext(Dispatchers.IO) {
		val path = repository.getVideoPath(video)
			?: return@withContext
		val settings = settingsRepository.getSettings()
		arrayOf(settings.moviePlayer, "\"$path\"").runCommand(File(path).parentFile)
	}

	override fun close() {
		/* no-op */
	}
}