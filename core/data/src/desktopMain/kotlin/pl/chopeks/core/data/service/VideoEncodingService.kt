package pl.chopeks.core.data.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import pl.chopeks.core.data.repository.EncoderRepository
import pl.chopeks.core.ffmpeg.FfmpegManager
import pl.chopeks.core.ffmpeg.task.EncodingTask
import pl.chopeks.core.model.EncodeStatus
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
						val encoderTask = EncodingTask(file, outputFile, ffmpegManager)
						val progressJob = launch {
							encoderTask.progress.collect { p ->
								repository.updateProgress(file.nameWithoutExtension, p)
							}
						}
						encoderTask.run()
						progressJob.cancel()

						if (repository.verifyEncodedFile(file)) {
							repository.removeFile(file)
							repository.updateStatus(file.nameWithoutExtension, EncodeStatus.FinishedAndRemoved)
							repository.refreshFiles()
						} else {
							repository.updateStatus(file.nameWithoutExtension, EncodeStatus.Error("Verification Failed"))
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