package pl.chopeks.core.data.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import pl.chopeks.core.data.repository.EncoderRepository
import pl.chopeks.core.ffmpeg.FfmpegManager
import pl.chopeks.core.ffmpeg.task.EncodingTask
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.minutes

class VideoEncodingService(
	private val repository: EncoderRepository,
	private val ffmpegManager: FfmpegManager,
	private val scope: CoroutineScope
) : IVideoEncodingService {
	private val isStarted = AtomicBoolean(false)

	override suspend fun startQueue() {
		if (!isStarted.compareAndSet(false, true))
			return
		scope.launch {
			try {
				while (scope.isActive) {
					val files = repository.getFilesToEncode()
					files.forEach { file ->
						val outputFile = File(repository.getSink(), "${file.nameWithoutExtension}.HEVC.mp4")
						EncodingTask(file, outputFile, ffmpegManager).use { encoderTask ->
							val progressJob = launch {
								encoderTask.progress.collect { p ->
									repository.updateProgress(file.name, p)
								}
							}
							encoderTask.run()
							progressJob.cancel()
						}
					}
					delay(1.minutes)
				}
			} finally {
				isStarted.set(false)
			}
		}
	}
}