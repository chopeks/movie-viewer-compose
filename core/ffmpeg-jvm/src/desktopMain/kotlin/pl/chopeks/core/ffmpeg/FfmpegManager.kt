package pl.chopeks.core.ffmpeg

import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.TimeUnit

/**
 * Manages interaction with `ffmpeg` and `ffprobe` command-line tools for media processing.
 */
class FfmpegManager {
	/**
	 * Starts a ffmpeg process to stream audio fingerprint data from a video file.
	 *
	 * @param video The video file to process.
	 * @param start The start time in milliseconds for the fingerprint stream.
	 * @param duration The duration in milliseconds for the fingerprint stream.
	 * @return The started `Process`.
	 */
	fun getFingerprintStream(video: File, start: Int? = null, duration: Int? = null): Process {
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

		return ProcessBuilder(ffmpegCmd)
			.redirectError(ProcessBuilder.Redirect.DISCARD)
			.start()
	}

	/**
	 * Gets the duration of the audio stream in a video file using `ffprobe`.
	 *
	 * @param video The video file.
	 * @return The duration in seconds, or null if it cannot be determined.
	 */
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

	/**
	 * Gets the duration of the video file using `ffprobe`.
	 *
	 * @param video The video file.
	 * @return The duration in milliseconds.
	 */
	fun getVideoDuration(video: File) = try {
		arrayOf("ffprobe", "-v", "error", "-show_entries", "format=duration", "-of", "csv=p=0", video.absolutePath)
			.executeCommand(File("./"))
			?.let {
				val duration = it.toDouble()
				TimeUnit.SECONDS.toMillis(duration.toLong()) + ((duration - duration.toLong()) * 1000).toLong()
			} ?: 0
	} catch (e: Throwable) {
		e.printStackTrace()
		0
	}.let { it - (it % 1000) }

	/**
	 * Captures a screenshot from the video at a specified percentage of the total duration.
	 *
	 * @param video The video file.
	 * @param percent The percentage of the video duration where the screenshot should be taken (default is 110 which seems incorrect if it means % of duration, likely meant relative to some other metric or offset).
	 * @return The screenshot image data as a byte array.
	 */
	fun makeScreenshot(video: File, percent: Long = 110): ByteArray {
		val interval: Long = getVideoDuration(video) * percent / 1000L
		var bytes = byteArrayOf()
		arrayOf(
			"ffmpeg",
			"-ss", "${TimeUnit.MILLISECONDS.toHours(interval)}:${TimeUnit.MILLISECONDS.toMinutes(interval) % 60}:${TimeUnit.MILLISECONDS.toSeconds(interval) % 60}",
			"-i", video.absolutePath,
			"-vframes", "1",
			"-f", "image2pipe",
			"-vcodec", "mjpeg",
			"pipe:1"
		).runPipeCommand(ProcessBuilder.Redirect.DISCARD) {
			bytes = it.readBytes()
		}
		return bytes
	}

	/**
	 * Checks if the `ffmpeg` command-line tool is available in the system's PATH.
	 *
	 * @return `true` if `ffmpeg` is available, `false` otherwise.
	 */
	fun isFfmpegAvailable(): Boolean {
		return try {
			val process = ProcessBuilder("ffmpeg", "-version")
				.redirectOutput(ProcessBuilder.Redirect.DISCARD)
				.redirectError(ProcessBuilder.Redirect.DISCARD)
				.start()
			process.destroy()
			true
		} catch (e: IOException) {
			false
		}
	}

	/**
	 * Checks if the `ffprobe` command-line tool is available in the system's PATH.
	 *
	 * @return `true` if `ffprobe` is available, `false` otherwise.
	 */
	fun isFfprobeAvailable(): Boolean {
		return try {
			val process = ProcessBuilder("ffprobe", "-version")
				.redirectOutput(ProcessBuilder.Redirect.DISCARD)
				.redirectError(ProcessBuilder.Redirect.DISCARD)
				.start()
			process.destroy()
			true
		} catch (e: IOException) {
			false
		}
	}

	private fun Array<String>.executeCommand(workingDir: File): String? {
		return try {
			val proc = ProcessBuilder(*this)
				.directory(workingDir)
				.redirectOutput(ProcessBuilder.Redirect.PIPE)
				.redirectError(ProcessBuilder.Redirect.PIPE)
				.start()

			proc.waitFor(5, TimeUnit.SECONDS)
			proc.inputStream.bufferedReader().readText()
		} catch (e: IOException) {
			e.printStackTrace()
			null
		}
	}

	private fun Array<String>.runPipeCommand(errorRedirect: ProcessBuilder.Redirect = ProcessBuilder.Redirect.INHERIT, callback: (InputStream) -> Unit) {
		val process = ProcessBuilder(*this)
			.redirectError(errorRedirect)
			.start()
		process.inputStream.use(callback)
		process.waitFor()
	}
}