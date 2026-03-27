package pl.chopeks.core.ffmpeg.task

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import pl.chopeks.core.ffmpeg.FfmpegManager
import java.io.Closeable
import java.io.File

class EncodingTask(
	val inputFile: File,
	val outputFile: File,
	private val ffmpeg: FfmpegManager
) : Closeable {
	private var process: Process? = null
	private val _progress = MutableStateFlow(0.0f)
	val progress = _progress.asStateFlow()

	suspend fun run() = withContext(Dispatchers.IO) {
		ffmpeg.encodeWithProgress(inputFile, outputFile) { _progress.value = it }
	}

	override fun close() {
		process?.destroy()
	}
}