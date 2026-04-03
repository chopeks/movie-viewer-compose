package pl.chopeks.movies.platform

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import pl.chopeks.core.data.IVideoPlayer
import pl.chopeks.core.data.repository.ISettingsRepository
import pl.chopeks.core.data.repository.IVideoRepository
import pl.chopeks.core.model.Video
import java.io.File

class VideoPlayer(
	private val repository: IVideoRepository,
	private val settingsRepository: ISettingsRepository,
	private val processFactory: (List<String>, ProcessBuilder.() -> ProcessBuilder) -> Process = { list, builder ->
		builder(ProcessBuilder(list)).start()
	}
) : IVideoPlayer {
	override suspend fun play(video: Video) = withContext(Dispatchers.IO) {
		val path = repository.getVideoPath(video)
			?: return@withContext
		val settings = settingsRepository.getSettings().first()
		arrayOf(settings.moviePlayer, "\"$path\"")
			.runCommand(File(path).parentFile, ProcessBuilder.Redirect.DISCARD)
	}

	private fun Array<String>.runCommand(workingDir: File, errorRedirect: ProcessBuilder.Redirect = ProcessBuilder.Redirect.INHERIT) {
		processFactory(toList()) {
			directory(workingDir)
				.redirectOutput(ProcessBuilder.Redirect.DISCARD)
				.redirectError(errorRedirect)
		}.waitFor()
	}
}