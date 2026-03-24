package pl.chopeks.movies.tasks

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import pl.chopeks.core.data.IImageConverter
import pl.chopeks.core.database.datasource.DirectoriesLocalDataSource
import pl.chopeks.core.database.datasource.VideoLocalDataSource
import pl.chopeks.core.database.datasource.VideoLocalDataSource.NewVideo
import pl.chopeks.core.ffmpeg.FfmpegManager
import pl.chopeks.core.model.Path
import pl.chopeks.core.model.Video
import pl.chopeks.core.utils.getDirectories
import pl.chopeks.core.utils.getFiles
import pl.chopeks.movies.utils.AppLogger
import java.io.File
import kotlin.system.measureTimeMillis

class VideoLookupTask(
	private val directoryDataSource: DirectoriesLocalDataSource,
	private val videoDataSource: VideoLocalDataSource,
	private val imageConverter: IImageConverter,
	private val ffmpegManager: FfmpegManager
) {
	private var isRunning = false

	suspend fun run(onEvent: (String) -> Unit = AppLogger::log) {
		if (isRunning)
			return
		isRunning = true
		refresh(onEvent)
		isRunning = false
	}

	suspend fun refresh(onEvent: (String) -> Unit) {
		val paths = directoryDataSource.getPaths()
		paths.forEach { path ->
			val directory = File(path.path)

			ensureDumpExists(directory)

			if (shouldCheckFiles(path)) {
				onEvent("File count different, checking which files were removed or added. (${path.path})")
				withContext(Dispatchers.Default) {
					val files = getDirectories(directory).map { dir ->
						async {
							val dbPaths = videoDataSource.getFilesInPath(dir.absolutePath)
							getFiles(dir).filter { it.absolutePath !in dbPaths }
						}
					}.awaitAll()
						.flatten()
						.sortedBy { it.absolutePath }
						.map { NewVideo(it.nameWithoutExtension, it.absolutePath) }

					videoDataSource.addNewVideos(files)

					files.forEach { onEvent("Added ${it.absolutePath}.") }
				}
			}

			withContext(Dispatchers.Default) {
				videoDataSource.getVideosWithoutThumbnail().map { video ->
					async {
						try {
							measureTimeMillis {
								val img = imageConverter.makeBase64Screenshot(video.second)
								videoDataSource.setImage(Video(video.first, "", 0), img)
							}.let { time -> println("${time}ms") }
							onEvent("Added thumbnail of ${video.second}.")
						} catch (e: Throwable) {
							e.printStackTrace()
						}
					}
				}.awaitAll()

				videoDataSource.getVideosWithoutDuration().map { video ->
					async {
						ffmpegManager.getVideoDuration(File(video.second)).also { dur ->
							videoDataSource.setDuration(video.first, dur.toInt())
						}
						onEvent("Added duration of ${video.second}.")
					}
				}.awaitAll()
				onEvent("Directory ${path.path} check completed.")
			}
		}
//		audioDedupLocalDataSource.reset()
	}

	suspend fun shouldCheckFiles(path: Path): Boolean {
		val dir = File(path.path)
		val fileCount = getFiles(dir).size.toLong()
		val dbCount = videoDataSource.countVideosInPath(dir.absolutePath)
		return dbCount != fileCount
	}

	fun ensureDumpExists(dir: File) {
		val dump = File(dir, ".dump")
		dump.parentFile.mkdirs()
		dump.mkdirs()
	}
}