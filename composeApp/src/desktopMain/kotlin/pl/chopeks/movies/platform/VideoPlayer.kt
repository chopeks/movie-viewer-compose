package pl.chopeks.movies.platform

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pl.chopeks.core.data.repository.ISettingsRepository
import pl.chopeks.core.data.repository.IVideoRepository
import pl.chopeks.core.model.Video
import pl.chopeks.movies.IVideoPlayer
import java.io.File
import java.util.concurrent.TimeUnit

class VideoPlayer(
	private val repository: IVideoRepository,
	private val settingsRepository: ISettingsRepository,
) : IVideoPlayer {
	override suspend fun play(video: Video) = withContext(Dispatchers.IO) {
		val path = repository.getVideoPath(video)
			?: return@withContext
		val settings = settingsRepository.getSettings()
		arrayOf(settings.moviePlayer, "\"$path\"")
			.runCommand(File(path).parentFile, ProcessBuilder.Redirect.DISCARD)
	}

	private fun Array<String>.runCommand(workingDir: File, errorRedirect: ProcessBuilder.Redirect = ProcessBuilder.Redirect.INHERIT) {
		ProcessBuilder(*this)
			.directory(workingDir)
			.redirectOutput(ProcessBuilder.Redirect.INHERIT)
			.redirectError(errorRedirect)
			.start()
			.waitFor(1, TimeUnit.SECONDS)
	}
}