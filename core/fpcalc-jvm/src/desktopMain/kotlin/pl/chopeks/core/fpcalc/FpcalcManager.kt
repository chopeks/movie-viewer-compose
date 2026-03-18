package pl.chopeks.core.fpcalc

import pl.chopeks.core.ffmpeg.FfmpegManager
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import kotlin.concurrent.thread

/**
 * Manages interaction with the `fpcalc` command-line tool to generate audio fingerprints.
 */
class FpcalcManager(
	private val ffmpegManager: FfmpegManager
) {
	/**
	 * Generates an audio fingerprint for the given video file.
	 *
	 * @param video The video file to process.
	 * @param start The start time in milliseconds from which to generate the fingerprint.
	 * @param duration The duration in milliseconds for which to generate the fingerprint.
	 * @return An array of unsigned integers representing the audio fingerprint, or `null` if an error occurs.
	 */
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

	/**
	 * Checks if the `fpcalc` command-line tool is available in the system's PATH.
	 *
	 * @return `true` if `fpcalc` is available, `false` otherwise.
	 */
	fun isFpcalcAvailable(): Boolean {
		return try {
			val process = ProcessBuilder("fpcalc", "-version")
				.redirectOutput(ProcessBuilder.Redirect.DISCARD)
				.redirectError(ProcessBuilder.Redirect.DISCARD)
				.start()
			process.destroy()
			true
		} catch (e: IOException) {
			false
		}
	}
}