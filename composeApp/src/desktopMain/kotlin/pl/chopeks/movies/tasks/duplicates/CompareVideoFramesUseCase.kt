package pl.chopeks.movies.tasks.duplicates

import kotlinx.coroutines.*
import pl.chopeks.core.database.duplicates.VideoDedupLocalDataStorage
import pl.chopeks.core.ffmpeg.VideoComparator
import pl.chopeks.movies.utils.AppLogger
import java.io.File

class CompareVideoFramesUseCase(
	private val datasource: VideoDedupLocalDataStorage,
	private val videoComparator: VideoComparator
) {
	/**
	 * @return false if there are no more movies to be checked
	 * @return true if a movie was checked and is ready to check next
	 */
	suspend fun run(threshold: Int = 0): Boolean = withContext(Dispatchers.Default) {
		val video = datasource.nextVideo()
			?: return@withContext false

		AppLogger.log("checking video duplicates for ${video.id} (${convertMillisToDuration(video.duration)}) ${video.path.absolutePath}")

		val candidates = datasource.getCandidates(video, threshold)
		if (candidates.isEmpty())
			return@withContext cleanUp(video.id)

		AppLogger.log("found ${candidates.size} possible duplicates for ${video.path.absolutePath}, checking now")

		return@withContext checkMovie(video.copy(candidates = candidates))
	}

	@OptIn(DelicateCoroutinesApi::class)
	private suspend fun checkMovie(model: VideoDedupLocalDataStorage.PossibleDuplicate): Boolean = withContext(Dispatchers.Default) {
		val mainPath = datasource.getPath(model.id)!!
		if (model.candidates.isNotEmpty()) {
			model.candidates.map { candidate ->
				async {
					val path = datasource.getPath(candidate) ?: return@async
					val result = videoComparator.compareVideos(File(mainPath), File(path))
					AppLogger.log("for $candidate $result")
					if (result.isValid) {
						datasource.addDuplicate(model.id, candidate)
						AppLogger.log("added ${model.id} -> $candidate to possible duplicates")
					}
				}
			}.awaitAll()
		}
		return@withContext cleanUp(model.id)
	}

	private suspend fun cleanUp(movieId: Int): Boolean {
		datasource.removeDuplicate(movieId)
		return true
	}

	private fun convertMillisToDuration(millis: Int?): String {
		if (millis == null)
			return ""

		val hours = millis / 3600000
		val minutes = (millis % 3600000) / 60000
		val seconds = (millis % 60000) / 1000

		return buildString {
			if (hours > 0) append("$hours:")
			append("${if (minutes < 10) "0$minutes" else minutes}:")
			append("${if (seconds < 10) "0$seconds" else seconds}")
		}
	}
}