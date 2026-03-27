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
import pl.chopeks.core.ffmpeg.VideoComparator
import pl.chopeks.core.model.EncodeStatus
import java.io.File

class EncoderRepository(
	private val settingsLocalDataSource: SettingsLocalDataSource,
	private val ffmpegManager: FfmpegManager,
	private val videoComparator: VideoComparator,
	scope: CoroutineScope
) : IEncoderRepository {
	private val _encodingStatus = MutableStateFlow<Map<String, EncodeStatus>>(emptyMap())

	init {
		scope.launch {
			refreshFiles()
		}
	}

	override fun observeEncodingStatus() = _encodingStatus.asStateFlow()

	internal suspend fun refreshFiles() = withContext(Dispatchers.IO) {
		val sourceMap = getFilesFromSource().associateBy { it.nameWithoutExtension }
		val sinkFiles = getSink().listFiles()?.filter { it.isFile && it.name.endsWith(".HEVC.mp4") } ?: emptyList()
		val sinkNames = sinkFiles.map { it.name.removeSuffix(".HEVC.mp4") }.toSet()
		val sourceNames = sourceMap.keys

		val onlyInSink = sinkNames - sourceNames
		val onlyInSource = sourceNames - sinkNames
		val inBoth = sinkNames intersect sourceNames

		_encodingStatus.update { current ->
			val nextMap = current.toMutableMap()

			// sink only: definitely removed source.
			onlyInSink.forEach { name ->
				if (nextMap[name] !is EncodeStatus.FinishedAndRemoved) {
					nextMap[name] = EncodeStatus.FinishedAndRemoved
				}
			}
			// source only: waiting to be processed
			onlyInSource.forEach { name ->
				val status = nextMap[name]
				if (status !is EncodeStatus.Processing && status !is EncodeStatus.Error) {
					nextMap[name] = EncodeStatus.Waiting
				}
			}

			inBoth.forEach { name ->
				val status = nextMap[name]
				val sourceFile = sourceMap[name]!!
				when (status) {
					is EncodeStatus.FinishedAndRemoved,  // Stay FinishedAndRemoved, do nothing
					is EncodeStatus.Processing, // Stay Processing, let the manager finish
					is EncodeStatus.Error -> Unit // Stay Error until manual retry
					else -> {
						if (verifyEncodedFile(sourceFile)) {
							removeFile(sourceFile)
							nextMap[name] = EncodeStatus.FinishedAndRemoved
						} else {
							nextMap[name] = EncodeStatus.Waiting
						}
					}
				}
			}
			nextMap.keys.retainAll { it in sourceNames || it in sinkNames }
			nextMap
		}
	}

	internal fun updateProgress(name: String, progress: Float) {
		_encodingStatus.update { currentMap ->
			if (progress > 0.999) {
				currentMap + (name to (EncodeStatus.Finished))
			} else {
				currentMap + (name to (EncodeStatus.Processing(progress)))
			}
		}
	}

	internal fun updateStatus(name: String, status: EncodeStatus) {
		_encodingStatus.update { currentMap -> currentMap + (name to status) }
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
		return File(settings.encoderSource).listFiles().filter { it.isFile }
	}

	internal fun getSink(): File {
		val settings = settingsLocalDataSource.get()
		return File(settings.encoderSink)
	}

	internal suspend fun verifyEncodedFile(file: File): Boolean {
		val targetFile = File(getSink(), "${file.nameWithoutExtension}.HEVC.mp4")

		val duration = ffmpegManager.getVideoDuration(file)
		val expectedDuration = ffmpegManager.getVideoDuration(targetFile)
		val durationCheck = duration == expectedDuration
		if (!durationCheck) {
			println("durationCheck failed for file ${file.name}")
		}
		val compareResult = videoComparator.compareVideos(file, targetFile)
		val compareCheck = compareResult.ssim > 0.9 && compareResult.psnr > 20.0
		if (!compareCheck) {
			println("compareCheck failed for file ${file.name} -> $compareResult")
		}
		return durationCheck && compareCheck
	}

	internal fun removeFile(file: File) {
		// todo though do not remove for now, until 100% certain it works
		val dumpDir = File(file.parent, ".dump")
		if (!dumpDir.exists()) {
			dumpDir.mkdirs()
		}

		val target = File(dumpDir, file.name)
		if (target.exists())
			target.delete()

		val success = file.renameTo(target)
		if (!success) {
			// Fallback if rename fails (e.g., across different partitions)
			file.copyTo(target, overwrite = true)
			file.delete()
		}
	}
}