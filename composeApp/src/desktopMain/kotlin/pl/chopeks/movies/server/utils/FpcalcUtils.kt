package pl.chopeks.movies.server.utils

import pl.chopeks.movies.utils.AppLogger
import java.io.File

object FpcalcUtils {
	@OptIn(ExperimentalUnsignedTypes::class)
	fun getFingerprint(video: File, start: Int? = null, duration: Int? = null): UIntArray? {
		val ffmpegCmd = mutableListOf("ffmpeg")
		if (start != null)
			ffmpegCmd += listOf("-ss", (start / 1000.0).toString())

		ffmpegCmd += listOf("-i", video.absolutePath)

		if (duration != null)
			ffmpegCmd += listOf("-t", (duration / 1000.0).toString())

		ffmpegCmd += listOf(
			"-vn",
			"-ac", "1",
			"-ar", "16000",
			"-f", "s16le",
			"-"
		)

		val fpcalcCmd = listOf(
			"fpcalc",
			"-format", "s16le",
			"-rate", "16000",
			"-raw",
			"-length", "0",
			"-"
		)


		val ffmpeg = ProcessBuilder(ffmpegCmd)
			.redirectError(ProcessBuilder.Redirect.DISCARD)
			.start()

		val fpcalc = ProcessBuilder(fpcalcCmd)
			.redirectError(ProcessBuilder.Redirect.DISCARD)
			.start()

		ffmpeg.inputStream.pipe(fpcalc.outputStream)

		val output = fpcalc.inputStream.bufferedReader().use { it.readText() }.trim()

		ffmpeg.destroy()
		fpcalc.destroy()

		if (output.isBlank()) {
			val duration = getAudioDuration(video)
				?: return null
			AppLogger.log("empty output - failed audio duration $duration, from file: ${video.absolutePath}")
			return UIntArray(0)
		}

		val fingerprintLine = output
			.lineSequence()
			.firstOrNull { it.startsWith("FINGERPRINT=") }

		if (fingerprintLine == null) {
			val duration = getAudioDuration(video)
				?: return null
			AppLogger.log("failed audio duration $duration, from file: ${video.absolutePath}")
			return UIntArray(0)
		}

		val fingerprint = fingerprintLine.removePrefix("FINGERPRINT=").split(",")
		return UIntArray(fingerprint.size) {
			fingerprint[it].toUInt()
		}
	}

	fun getAudioDuration(video: File): Double? {
		val process = ProcessBuilder(
			listOf(
				"ffprobe",
				"-v", "error",
				"-select_streams", "a",
				"-show_entries", "stream=duration",
				"-of", "default=noprint_wrappers=1:nokey=1",
				video.absolutePath
			)
		)
			.redirectError(ProcessBuilder.Redirect.DISCARD)
			.start()

		val output = process.inputStream.bufferedReader().readText().trim()
		process.waitFor()

		if (output.isBlank() || output == "N/A")
			return 0.0

		return output.toDoubleOrNull()
	}
}