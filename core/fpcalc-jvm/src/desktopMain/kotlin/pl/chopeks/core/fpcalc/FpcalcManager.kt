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
	private val ffmpegManager: FfmpegManager,
	private val processFactory: (List<String>, ProcessBuilder.() -> ProcessBuilder) -> Process = { list, builder ->
		builder(ProcessBuilder(list)).start()
	}
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
		val audioByteArray = ffmpegManager.getFingerprintStream(video, start, duration)

		val fpcalcCmd = listOf(
			"fpcalc",
			"-format", "s16le",
			"-rate", "16000",
			"-raw",
			"-length", "0",
			"-"
		)

		val fpcalc = ProcessBuilder(fpcalcCmd)
			.redirectError(ProcessBuilder.Redirect.DISCARD)
			.start()

		fpcalc.outputStream.use { it.write(audioByteArray) }

		val output = fpcalc.inputStream.bufferedReader().use { it.readText() }.trim()

		fpcalc.destroy()

		if (output.isBlank()) {
			val duration = ffmpegManager.getAudioDuration(video)
				?: return null
			println("empty output - failed audio duration $duration, from file: ${video.absolutePath}")
			return UIntArray(0)
		}

		val result = parseFingerprint(output)

		if (result != null && result.isNotEmpty())
			return result

		val audioDuration = ffmpegManager.getAudioDuration(video)
			?: return null
		println("${if (output.isBlank()) "empty output" else "failed"} - audio duration $audioDuration, file: ${video.absolutePath}")
		return UIntArray(0)
	}

	@OptIn(ExperimentalUnsignedTypes::class)
	internal fun parseFingerprint(output: String): UIntArray? {
		val line = output.lineSequence().firstOrNull { it.startsWith("FINGERPRINT=") }
			?: return null
		val parts = line.removePrefix("FINGERPRINT=").split(",")
		if (parts.isEmpty() || parts[0].isBlank())
			return UIntArray(0)

		return UIntArray(parts.size) {
			parts[it].toUInt()
		}
	}


	/**
	 * Checks if the `fpcalc` command-line tool is available in the system's PATH.
	 *
	 * @return `true` if `fpcalc` is available, `false` otherwise.
	 */
	fun isFpcalcAvailable(): Boolean {
		return try {
			val process = processFactory(listOf("fpcalc", "-version")) {
				redirectOutput(ProcessBuilder.Redirect.DISCARD)
					.redirectError(ProcessBuilder.Redirect.DISCARD)
			}
			process.waitFor() == 0
		} catch (e: IOException) {
			false
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