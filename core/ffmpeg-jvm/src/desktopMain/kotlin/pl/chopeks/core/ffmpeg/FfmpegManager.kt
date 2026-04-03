package pl.chopeks.core.ffmpeg

import kotlinx.coroutines.flow.flow
import pl.chopeks.core.ffmpeg.model.FfmpegCapabilities
import pl.chopeks.core.ffmpeg.utils.formatDurationToFfmpegFormat
import pl.chopeks.core.ffmpeg.utils.runCancellable
import pl.chopeks.core.model.capability.Capability
import pl.chopeks.core.model.capability.CapabilityGuard
import pl.chopeks.core.utils.ensure
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
	companion object {
		private const val FFMPEG_COMMAND = "ffmpeg"
		private const val VAMF_MODEL_FILE = "vmaf_v0.6.1.json"
		private val VMAF_REGEX = Regex("VMAF score: (\\d+\\.\\d+)")
		private val FFMPEG_VERSION_REGEX = Regex("version (\\d+\\.\\d+)")
	}

	private val workDir = File(System.getProperty("java.io.tmpdir"), "ffmpeg")

	init {
		if (!workDir.exists())
			workDir.mkdirs()
		val modelFile = File(workDir, VAMF_MODEL_FILE)
		if (!modelFile.exists())
			Unit.javaClass.getResourceAsStream("/$VAMF_MODEL_FILE").use { it.copyTo(modelFile.outputStream()) }
	}

	/**
	 * Starts a ffmpeg process to stream audio fingerprint data from a video file.
	 *
	 * @param video The video file to process.
	 * @param start The start time in milliseconds for the fingerprint stream.
	 * @param duration The duration in milliseconds for the fingerprint stream.
	 * @return The started `Process`.
	 */
	context(guard: CapabilityGuard)
	fun getFingerprintStream(video: File, start: Int? = null, duration: Int? = null): Process {
		ensure(Capability.VIDEO_ENGINE)

		val ffmpegCmd = mutableListOf(FFMPEG_COMMAND)
		if (start != null)
			ffmpegCmd += listOf("-ss", formatDurationToFfmpegFormat(start.toLong()))

		ffmpegCmd += listOf("-i", video.absolutePath)

		if (duration != null)
			ffmpegCmd += listOf("-t", formatDurationToFfmpegFormat(duration.toLong()))

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

		val output = process.inputStream.bufferedReader().use { it.readText() }.trim()
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
			val process = processFactory(listOf(FFMPEG_COMMAND, "-version")) {
				redirectError(ProcessBuilder.Redirect.DISCARD)
			}
			process.waitFor() == 0
		} catch (e: IOException) {
			false
		}
	}

	fun getFfmpegCapabilities(): FfmpegCapabilities? {
		return try {
			val process = processFactory(listOf(FFMPEG_COMMAND, "-version")) {
				redirectError(ProcessBuilder.Redirect.DISCARD)
			}
			val output = process.inputStream.bufferedReader().use { it.readText() }

			process.destroy()

			val versionMatch = FFMPEG_VERSION_REGEX.find(output)?.groupValues?.get(1) ?: "unknown"

			val configLine = output.lines().find { it.startsWith("configuration:") } ?: ""

			FfmpegCapabilities(
				version = versionMatch,
				hasVmaf = configLine.contains("--enable-libvmaf"),
				hasAmf = configLine.contains("--enable-amf"),
				hasNvenc = configLine.contains("--enable-nvenc"),
				hasLibx265 = configLine.contains("--enable-libx265")
			)
		} catch (e: Exception) {
			null
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

	fun getFfprobeVersion(): String? {
		return try {
			val process = processFactory(listOf(FFMPEG_COMMAND, "-version")) {
				redirectError(ProcessBuilder.Redirect.DISCARD)
			}
			val output = process.inputStream.bufferedReader().use { it.readText() }
			process.destroy()
			FFMPEG_VERSION_REGEX.find(output)?.groupValues?.get(1)
		} catch (e: Exception) {
			null
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
			proc.inputStream.bufferedReader().use { it.readText() }
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

	context(guard: CapabilityGuard)
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
	context(guard: CapabilityGuard)
	internal fun getFrame(video: File, permille: Long, width: Int = 0, height: Int = 0): BufferedImage? {
		ensure(Capability.VIDEO_ENGINE)

		val timestampMs = (getVideoDuration(video) * permille) / 1000L
		val cmd = mutableListOf(
			FFMPEG_COMMAND,
			"-ss", formatDurationToFfmpegFormat(timestampMs),
			"-i", video.absolutePath,
			"-vframes", "1"
		)
		if (width > 0 && height > 0) {
			cmd += listOf("-vf", "scale=$width:$height")
		}
		cmd += listOf(
			"-f", "image2pipe",
			"-vcodec", "mjpeg",
			"-"
		)

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

	context(guard: CapabilityGuard)
	fun availableEncoders(): List<String> {
		ensure(Capability.VIDEO_ENGINE)

		val process = processFactory(listOf(FFMPEG_COMMAND, "-encoders")) {
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
			FFMPEG_COMMAND, "-y",
			"-t", "0.1",
			"-f", "lavfi", "-i", "color=c=black:s=1280x720:r=24",
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

	fun encodeWithProgress(file: File, newFile: File) = flow {
		val totalDurationUs = getVideoDuration(file) * 1000L
		val cmd = mutableListOf(FFMPEG_COMMAND, "-y", "-i", file.absolutePath).apply {
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
			inputStream.bufferedReader().lineSequence()
				.filter { it.startsWith("out_time_us=") }
				.map { line ->
					val currentTimeUs = line.substringAfter("=").toLongOrNull() ?: 0L
					if (totalDurationUs > 0) {
						(currentTimeUs.toFloat() / totalDurationUs).coerceIn(0f, 1f)
					} else 0f
				}
				.forEach { emit(it) }
		}
	}

	/**
	 * Runs a 5-second VMAF quality check comparing an encoded video to its source.
	 * Uses fast-seeking for speed and a normalized filter chain for accuracy.
	 *
	 * @param original The source video file.
	 * @param output The encoded HEVC file.
	 * @param startTimeMs The timestamp to jump to for the sample (e.g., middle of video).
	 * @return The VMAF score as a Double, or 0.0 if the check fails.
	 */
	context(guard: CapabilityGuard)
	fun getVmafScore(
		original: File,
		output: File,
		startTimeMs: Long
	): Double {
		ensure(Capability.VIDEO_VMAF)

		val width = getResolutionEntry(output, "width") ?: return 0.0
		val height = getResolutionEntry(output, "height") ?: return 0.0

		val ffmpegCmd = listOf(
			FFMPEG_COMMAND,
			"-ss", formatDurationToFfmpegFormat(startTimeMs), "-t", "10", "-i", output.absolutePath,
			"-ss", formatDurationToFfmpegFormat(startTimeMs), "-t", "10", "-i", original.absolutePath,
			"-filter_complex",
			"[0:v]scale=$width:$height:flags=neighbor,setsar=1,format=yuv420p[dist];" +
				"[1:v]scale=$width:$height:flags=neighbor,setsar=1,format=yuv420p[ref];" +
				"[dist][ref]libvmaf=model='path=$VAMF_MODEL_FILE':n_threads=4",
			"-f", "null", "-"
		)
		println("vmaf command ${ffmpegCmd.joinToString(" ")}")

		return try {
			val process = processFactory(ffmpegCmd) {
				directory(workDir)
				redirectError(ProcessBuilder.Redirect.PIPE)
			}

			val outputText = process.errorStream.bufferedReader().readText()
			process.waitFor()

			val match = VMAF_REGEX.find(outputText)

			match?.groupValues?.get(1)?.toDouble() ?: 0.0
		} catch (e: Exception) {
			0.0
		}
	}

	/**
	 * Helper to get specific video stream metadata (width/height) using ffprobe.
	 */
	private fun getResolutionEntry(video: File, entry: String): Int? {
		val cmd = listOf(
			"ffprobe", "-v", "error",
			"-select_streams", "v:0",
			"-show_entries", "stream=$entry",
			"-of", "default=noprint_wrappers=1:nokey=1",
			video.absolutePath
		)

		return try {
			val process = Runtime.getRuntime().exec(cmd.toTypedArray())
			val result = process.inputStream.bufferedReader().readText().trim()
			process.waitFor()
			result.toIntOrNull()
		} catch (e: Exception) {
			null
		}
	}
}