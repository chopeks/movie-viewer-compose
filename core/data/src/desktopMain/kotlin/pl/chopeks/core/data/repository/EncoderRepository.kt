package pl.chopeks.core.data.repository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pl.chopeks.core.database.datasource.SettingsLocalDataSource
import pl.chopeks.core.ffmpeg.FfmpegManager
import pl.chopeks.core.model.EncodedVideo
import java.io.File

class EncoderRepository(
	private val settingsLocalDataSource: SettingsLocalDataSource,
	private val ffmpegManager: FfmpegManager,
	scope: CoroutineScope
) : IEncoderRepository {
	private val _encodingStatus = MutableStateFlow<Map<String, Float>>(emptyMap())

	init {
		scope.launch {
			_encodingStatus.emit(getFiles().associate { it.fileName to it.progress })
		}
	}

	override fun observeEncodingStatus() = _encodingStatus.asStateFlow()

	internal suspend fun getFiles(): List<EncodedVideo> = withContext(Dispatchers.IO) {
		val settings = settingsLocalDataSource.get()
		val sourceFiles = getFilesFromSource()
		val targetDir = File(settings.encoderSink)

		return@withContext sourceFiles.map { file ->
			val duration = ffmpegManager.getVideoDuration(file)
			val expectedDuration = ffmpegManager.getVideoDuration(File(targetDir, "${file.nameWithoutExtension}.HEVC.mp4"))
			EncodedVideo(file.name, expectedDuration.toFloat() / duration)
		}
	}

	internal fun updateProgress(name: String, p: Float) {
		_encodingStatus.update { currentMap ->
			currentMap + (name to p)
		}
	}

	internal fun getFilesToEncode(): List<File> {
		val settings = settingsLocalDataSource.get()
		val targetDir = File(settings.encoderSink)
		return getFilesFromSource()
			.filter { file ->
				val duration = ffmpegManager.getVideoDuration(file)
				val expectedDuration = ffmpegManager.getVideoDuration(File(targetDir, "${file.nameWithoutExtension}.HEVC.mp4"))
				duration != expectedDuration
			}
	}

	internal fun getFilesFromSource(): List<File> {
		val settings = settingsLocalDataSource.get()
		return File(settings.encoderSource).listFiles().toList()
	}

	internal fun getSink(): File {
		val settings = settingsLocalDataSource.get()
		return File(settings.encoderSink)
	}
}