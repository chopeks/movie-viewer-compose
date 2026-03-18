package com.chopeks.pl.chopeks.core.fpcalc

import com.chopeks.pl.chopeks.core.ffmpeg.FfmpegManager
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import kotlin.concurrent.thread

class FpcalcManager(
	private val ffmpegManager: FfmpegManager
) {
	@OptIn(ExperimentalUnsignedTypes::class)
	fun getFingerprint(video: File, start: Int? = null, duration: Int? = null): UIntArray? {
		val fpcalcCmd = listOf(
			"fpcalc",
			"-format", "s16le",
			"-rate", "16000",
			"-raw",
			"-length", "0",
			"-"
		)

		val ffmpeg = ffmpegManager.getFingerprintStream(video, start, duration)

		val fpcalc = ProcessBuilder(fpcalcCmd)
			.redirectError(ProcessBuilder.Redirect.DISCARD)
			.start()

		ffmpeg.inputStream.pipe(fpcalc.outputStream)

		val output = fpcalc.inputStream.bufferedReader().use { it.readText() }.trim()

		ffmpeg.destroy()
		fpcalc.destroy()

		if (output.isBlank()) {
			val duration = ffmpegManager.getAudioDuration(video)
				?: return null
			println("empty output - failed audio duration $duration, from file: ${video.absolutePath}")
			return UIntArray(0)
		}

		val fingerprintLine = output
			.lineSequence()
			.firstOrNull { it.startsWith("FINGERPRINT=") }

		if (fingerprintLine == null) {
			val duration = ffmpegManager.getAudioDuration(video)
				?: return null
			println("failed audio duration $duration, from file: ${video.absolutePath}")
			return UIntArray(0)
		}

		val fingerprint = fingerprintLine.removePrefix("FINGERPRINT=").split(",")
		return UIntArray(fingerprint.size) {
			fingerprint[it].toUInt()
		}
	}

	private fun InputStream.pipe(output: OutputStream) {
		thread {
			this.use { inp ->
				output.use { out ->
					inp.copyTo(out)
				}
			}
		}
	}
}