package pl.chopeks.core.ffmpeg

import pl.chopeks.core.ffmpeg.utils.ffmpeg
import pl.chopeks.core.ffmpeg.utils.linesFlow
import pl.chopeks.core.ffmpeg.utils.runCancellable
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.imageio.ImageIO

/**
 * Manages interaction with `ffmpeg` and `ffprobe` tools for media processing.
 */
class FfmpegManager(
	private val processFactory: (List<String>, ProcessBuilder.() -> ProcessBuilder) -> Process = { list, builder ->
		builder(ProcessBuilder(list)).start()
	}
) {
	init {
		println(availableEncoders())
	}

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

		return processFactory(ffmpegCmd) {
			redirectError(ProcessBuilder.Redirect.DISCARD)
		}
	}

	/**
	 * Gets the duration of the audio stream in a video file using `ffprobe`.
	 *
	 * @param video The video file.
	 * @return The duration in seconds, or null if it cannot be determined.
	 */
	fun getAudioDuration(video: File): Double? {
		val ffprobe = listOf(
			"ffprobe",
			"-v", "error",
			"-select_streams", "a",
			"-show_entries", "stream=duration",
			"-of", "default=noprint_wrappers=1:nokey=1",
			video.absolutePath
		)

		val process = processFactory(ffprobe) {
			redirectError(ProcessBuilder.Redirect.DISCARD)
		}

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

	fun getVideoDuration(video: File): Long = try {
		if (!video.exists())
			return 0L
		val ffmpegCmd = listOf(
			"ffprobe",
			"-v", "error",
			"-show_entries", "format=duration",
			"-of", "csv=p=0",
			video.absolutePath
		)

		val response = ffmpegCmd.executeCommand(File("./"))
			?: return 0L
		val duration = response.toDouble()

		TimeUnit.SECONDS.toMillis(duration.toLong()) + ((duration - duration.toLong()) * 1000).toLong()
			.let { it - (it % 1000) }
	} catch (e: Throwable) {
		0
	}

	/**
	 * Checks if the `ffmpeg` command-line tool is available in the system's PATH.
	 *
	 * @return `true` if `ffmpeg` is available, `false` otherwise.
	 */
	fun isFfmpegAvailable(): Boolean {
		return try {
			val process = processFactory(ffmpeg().version().build()) {
				redirectOutput(ProcessBuilder.Redirect.DISCARD)
					.redirectError(ProcessBuilder.Redirect.DISCARD)
			}
			process.waitFor() == 0
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
			val process = processFactory(listOf("ffprobe", "-version")) {
				redirectOutput(ProcessBuilder.Redirect.DISCARD)
					.redirectError(ProcessBuilder.Redirect.DISCARD)
			}
			process.waitFor() == 0
		} catch (e: IOException) {
			false
		}
	}

	private fun List<String>.executeCommand(workingDir: File): String? {
		return try {
			val proc = processFactory(this) {
				directory(workingDir)
					.redirectOutput(ProcessBuilder.Redirect.PIPE)
					.redirectError(ProcessBuilder.Redirect.PIPE)
			}
			proc.waitFor(5, TimeUnit.SECONDS)
			proc.inputStream.bufferedReader().readText()
		} catch (e: IOException) {
			e.printStackTrace()
			null
		}
	}

	/**
	 * Captures a screenshot from the video at a specified permille of the total duration.
	 *
	 * @param video The video file.
	 * @param permille The permille of the video duration where the screenshot should be taken
	 * @return The screenshot image data as a byte array.
	 */
	fun makeScreenshot(video: File, permille: Long = 110): ByteArray {
		return getFrame(video, permille)?.let {
			ByteArrayOutputStream().use { outputStream ->
				ImageIO.write(it, "jpg", outputStream)
				outputStream.toByteArray()
			}
		} ?: byteArrayOf()
	}

	/**
	 * Captures a frame from the video at a specified permille of the total duration.
	 *
	 * @param video The video file.
	 * @param permille The permille of the video duration where the screenshot should be taken
	 * @return The screenshot in form of buffered image.
	 */
	internal fun getFrame(video: File, permille: Long, width: Int = 0, height: Int = 0): BufferedImage? {
		val timestampUs = (getVideoDuration(video) * permille) / 1000L
		val cmd = ffmpeg()
			.seek(timestampUs)
			.input(video)
			.frames(1)
			.scale(width, height)
			.format("image2pipe")
			.videoCodec("mjpeg")
			.pipe()
			.build()

		val process = processFactory(cmd) {
			redirectError(ProcessBuilder.Redirect.DISCARD)
		}
		return try {
			process.inputStream.use {
				ImageIO.read(it)
			}
		} catch (e: Exception) {
			e.printStackTrace()
			null
		} finally {
			process.destroy()
		}
	}

	fun availableEncoders(): List<String> {
		val process = processFactory(ffmpeg().custom("-encoders").build()) {
			redirectError(ProcessBuilder.Redirect.DISCARD)
		}
		val output = process.inputStream.bufferedReader().readText()
		process.destroy()

		val list = output.split("------").lastOrNull()
			?: return emptyList()

		return list.split("\n").map {
			it.trim().split(" ").getOrElse(1) { "" }.trim()
		}.filter { it.isNotBlank() }
			.filter { "hevc" in it || "265" in it }
			.filter(::isEncoderActuallyWorking)
			.sorted()
	}

	private fun selectBestEncoder(available: List<String>): String {
		return when {
			"hevc_amf" in available -> "hevc_amf"
			"hevc_nvenc" in available -> "hevc_nvenc"
			"hevc_qsv" in available -> "hevc_qsv"
			else -> "libx265"
		}
	}

	internal fun isEncoderActuallyWorking(encoder: String): Boolean {
		val cmd = mutableListOf(
			"ffmpeg", "-y",
			"-t", "0.1",
			"-f", "lavfi", "-i", "color=c=black:s=1280x720:r=24", // Changed to 720p
			"-pix_fmt", "yuv420p",
			"-c:v", encoder
		)

		when (encoder) {
			"hevc_amf" -> cmd.addAll(listOf("-rc", "cqp", "-qp_i", "28", "-quality", "quality"))
			"hevc_nvenc" -> cmd.addAll(listOf("-rc", "vbr", "-cq", "28"))
			"hevc_qsv" -> cmd.addAll(listOf("-global_quality", "28"))
		}

		cmd.addAll(listOf("-f", "null", "-"))

		val process = processFactory(cmd) {
			redirectErrorStream(true)
		}

		val output = process.inputStream.bufferedReader().readText()
		val exitCode = process.waitFor()

//		if (exitCode != 0) {
//			println("Encoder $encoder failed: $output")
//		}

		return exitCode == 0
	}

	suspend fun encodeWithProgress(file: File, newFile: File, onProgress: (Float) -> Unit) {
		val totalDurationUs = getVideoDuration(file) * 1000L
		val cmd = mutableListOf("ffmpeg", "-y", "-i", file.absolutePath).apply {
			addAll(
				listOf(
					"-vf", "scale=-2:min(720\\,ih)",
					"-c:v", "hevc_amf",
					"-rc", "cqp",
					"-qp_i", "28",
					"-qp_p", "30",
					"-qp_b", "34",
					"-quality", "quality",
					"-c:a", "libmp3lame",
					"-b:a", "48k",
					"-movflags", "+faststart",
					"-progress", "pipe:1",
					"-nostats",
					newFile.absolutePath
				)
			)
		}

		processFactory(cmd) {
			redirectError(ProcessBuilder.Redirect.DISCARD)
		}.runCancellable {
			inputStream.bufferedReader().linesFlow().collect { line ->
				if (line.startsWith("out_time_us=")) {
					val currentTimeUs = line.substringAfter("=").toLongOrNull() ?: 0L
					if (totalDurationUs > 0) {
						onProgress((currentTimeUs.toFloat() / totalDurationUs).coerceIn(0f, 1f))
					}
				}
			}
		}
	}
}