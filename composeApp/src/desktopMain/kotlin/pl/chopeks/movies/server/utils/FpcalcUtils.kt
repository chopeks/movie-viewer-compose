package pl.chopeks.movies.server.utils

import java.io.File

object FpcalcUtils {
	@OptIn(ExperimentalUnsignedTypes::class)
	fun getFingerprint(video: File, start: Int? = null, duration: Int? = null): UIntArray? {
		val ffmpegCmd = mutableListOf("ffmpeg")
		if (start != null)
			ffmpegCmd += listOf("-ss", start.toString())

		ffmpegCmd += listOf("-i", video.absolutePath)

		if (duration != null)
			ffmpegCmd += listOf("-t", duration.toString())

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

		val output = fpcalc.inputStream.bufferedReader().readText()

		ffmpeg.waitFor()
		fpcalc.waitFor()

		if (output.isBlank())
			return null

		val fingerprintLine = output
			.lineSequence()
			.firstOrNull { it.startsWith("FINGERPRINT=") }
			?: return null

		return fingerprintLine
			.removePrefix("FINGERPRINT=")
			.split(',')
			.map { it.toUInt() }
			.toUIntArray()
	}
}