package pl.chopeks.core.ffmpeg

import org.bytedeco.ffmpeg.ffmpeg
import org.bytedeco.javacpp.Loader
import org.bytedeco.javacv.FFmpegFrameGrabber
import org.bytedeco.javacv.Java2DFrameConverter
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.ImageIO

/**
 * Manages interaction with `ffmpeg` and `ffprobe` command-line tools for media processing.
 */
class FfmpegManager(
	private val converter: Java2DFrameConverter = Java2DFrameConverter(),
	private val processFactory: (List<String>, ProcessBuilder.() -> ProcessBuilder) -> Process = { list, builder ->
		builder(ProcessBuilder(list)).start()
	}
) {
	private val ffmpegPath by lazy { Loader.load(ffmpeg::class.java) }

	/**
	 * Starts a ffmpeg process to stream audio fingerprint data from a video file.
	 *
	 * @param video The video file to process.
	 * @param start The start time in milliseconds for the fingerprint stream.
	 * @param duration The duration in milliseconds for the fingerprint stream.
	 * @return The started `Process`.
	 */
	fun getFingerprintStream(video: File, start: Int? = null, duration: Int? = null): Process {
		val ffmpegCmd = mutableListOf(ffmpegPath)
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
		return FFmpegFrameGrabber(video).use { grabber ->
			try {
				grabber.start()
				val audioStreamIdx = grabber.audioStream
				if (audioStreamIdx >= 0) {
					val stream = grabber.formatContext.streams(audioStreamIdx)
					val timeBase = stream.time_base()
					val durationInSeconds = stream.duration().toDouble() * timeBase.num() / timeBase.den()
					if (durationInSeconds <= 0)
						return@use null
					return@use durationInSeconds
				}
				null
			} catch (e: Exception) {
				null
			}
		}?.also { println("audio duration: ${video.absolutePath} -> ${it}s") }
	}

	/**
	 * Gets the duration of the video file using `ffprobe`.
	 *
	 * @param video The video file.
	 * @return The duration in milliseconds.
	 */
	fun getVideoDuration(video: File): Long {
		return FFmpegFrameGrabber(video).use { grabber ->
			try {
				grabber.start()
				val duration = grabber.lengthInTime / 1000 // us -> ms
				duration.let { it - (it % 1000) } // trim us
			} catch (e: Exception) {
				0L
			}
		}
	}

	/**
	 * Captures a screenshot from the video at a specified percentage of the total duration.
	 *
	 * @param video The video file.
	 * @param permille The permille of the video duration where the screenshot should be taken
	 * @return The screenshot image data as a byte array.
	 */
	fun makeScreenshot(video: File, permille: Long = 110): ByteArray {
		return FFmpegFrameGrabber(video).use { grabber ->
			try {
				grabber.start()
				grabber.timestamp = (grabber.lengthInTime * permille) / 1000L
				val frame = grabber.grabImage()
					?: return@use byteArrayOf()

				val bufferedImage = converter.convert(frame)
				ByteArrayOutputStream().use { outputStream ->
					ImageIO.write(bufferedImage, "jpg", outputStream)
					outputStream.toByteArray()
				}
			} catch (e: Exception) {
				e.printStackTrace()
				byteArrayOf()
			}
		}
	}
}