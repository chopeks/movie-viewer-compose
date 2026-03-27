package pl.chopeks.core.ffmpeg.task

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import pl.chopeks.core.ffmpeg.FfmpegManager
import java.io.File
import kotlin.coroutines.cancellation.CancellationException

class EncodingTask(
	val inputFile: File,
	val outputFile: File,
	private val ffmpeg: FfmpegManager
) {
	private val _progress = MutableStateFlow(0.0f)
	val progress = _progress.asStateFlow()

	suspend fun run() = withContext(Dispatchers.IO) {
		try {
			ffmpeg.encodeWithProgress(inputFile, outputFile) {
				_progress.value = it
			}
		} catch (e: CancellationException) {
			if (outputFile.exists())
				outputFile.delete()
			throw e
		}
	}
}