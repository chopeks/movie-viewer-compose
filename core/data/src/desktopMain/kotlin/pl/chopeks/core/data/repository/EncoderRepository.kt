package pl.chopeks.core.data.repository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pl.chopeks.core.database.datasource.SettingsLocalDataSource
import pl.chopeks.core.ffmpeg.FfmpegManager
import pl.chopeks.core.ffmpeg.VideoComparator
import pl.chopeks.core.model.EncodeStatus
import pl.chopeks.core.model.capability.FeatureNotSupportedException
import java.io.File
import kotlin.math.abs

class EncoderRepository(
	private val capabilityRepository: SystemCapabilityRepository,
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

	internal suspend fun getFilesToEncode(): List<File> {
		val settings = settingsLocalDataSource.get().first()
		val targetDir = File(settings.encoderSink)
		return getFilesFromSource()
			.filter { file ->
				val duration = ffmpegManager.getVideoDuration(file)
				val expectedDuration = ffmpegManager.getVideoDuration(File(targetDir, "${file.nameWithoutExtension}.HEVC.mp4"))
				duration != expectedDuration
			}
	}

	internal suspend fun getFilesFromSource(): List<File> {
		val settings = settingsLocalDataSource.get().first()
		val dir = File(settings.encoderSource)
		if (!dir.exists() || !dir.isDirectory) return emptyList()
		return dir.listFiles()?.filter { it.isFile } ?: emptyList()
	}

	internal suspend fun getSink(): File {
		val settings = settingsLocalDataSource.get().first()
		return File(settings.encoderSink)
	}

	internal suspend fun verifyEncodedFile(file: File): Boolean {
		val targetFile = File(getSink(), "${file.nameWithoutExtension}.HEVC.mp4")
		try {
			with(capabilityRepository::contains) {
				val duration = ffmpegManager.getVideoDuration(file)
				val expectedDuration = ffmpegManager.getVideoDuration(targetFile)
				val durationDiff = abs(expectedDuration - duration)
				val durationCheck = durationDiff <= 2000
				if (!durationCheck) {
					println("durationCheck failed for file ${file.name} -> $duration!=$expectedDuration, diff=${abs(expectedDuration - duration)}")
				}
				// compare videos is fast but brittle, so call it
				val compareResult = videoComparator.compareVideos(file, targetFile)
				val compareCheck = compareResult.ssim > 0.9 && compareResult.psnr > 20.0
				if (!compareCheck) {
					println("compareCheck failed for file ${file.name} -> $compareResult")
				}

				if (durationCheck && compareCheck)
					return true

				// vmaf is kinda slow, call it as last resort
				val vmafResult = ffmpegManager.getVmafScore(file, targetFile, duration / 2)
				println("vmaf result for file ${file.name} -> $vmafResult")
				return vmafResult > 85.0
			}
		} catch (e: FeatureNotSupportedException) {
			e.printStackTrace()
			return false
		} catch (e: Throwable) {
			e.printStackTrace()
			return false
		}
	}

	internal fun removeFile(file: File) {
		if (file.exists() && file.isFile) {
			file.delete()
		}
	}
}