package pl.chopeks.core.ffmpeg

import org.bytedeco.ffmpeg.global.avutil
import org.bytedeco.javacv.*
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ShortBuffer
import javax.imageio.ImageIO

/**
 * Manages interaction with `ffmpeg` and `ffprobe` tools for media processing.
 */
class FfmpegManager(
	private val converter: Java2DFrameConverter = Java2DFrameConverter()
) {
	init {
		avutil.av_log_set_level(avutil.AV_LOG_QUIET)
	}

	/**
	 * Fetches raw audio data from a video file.
	 *
	 * @param video The video file to process.
	 * @param start The start time in milliseconds.
	 * @param duration The duration in milliseconds.
	 * @return raw audio sample.
	 */
	fun getFingerprintStream(video: File, start: Int? = null, duration: Int? = null): ByteArray {
		val out = ByteArrayOutputStream()
		val sampleRate = 16000

		FFmpegFrameGrabber(video).use { grabber ->
			grabber.start()
			if (start != null) {
				grabber.timestamp = start * 1000L
			}
			FFmpegFrameFilter(
				"aresample=$sampleRate,aformat=sample_fmts=s16:channel_layouts=mono", grabber.audioChannels
			).use { filter ->
				filter.sampleRate = grabber.sampleRate
				filter.start()
				FFmpegFrameRecorder(out, 1).use { recorder ->
					recorder.format = "s16le"
					recorder.audioCodecName = "pcm_s16le"
					recorder.sampleRate = sampleRate
					recorder.audioChannels = 1
					recorder.start()

					val targetBytes = if (duration != null)
						(duration * sampleRate * 2) / 1000
					else
						Int.MAX_VALUE

					var writtenBytes = 0

					while (true) {
						val frame = grabber.grabSamples() ?: break

						filter.push(frame)

						var filtered: Frame?
						while (filter.pull().also { filtered = it } != null) {
							val f = filtered ?: break
							val samples = f.samples ?: continue

							val buffer = samples[0] as ShortBuffer
							val remainingSamples = buffer.remaining()
							val bytesInFrame = remainingSamples * 2

							if (writtenBytes + bytesInFrame > targetBytes) {
								val allowedSamples = (targetBytes - writtenBytes) / 2
								buffer.limit(buffer.position() + allowedSamples)
							}

							recorder.record(f)

							writtenBytes += minOf(bytesInFrame, targetBytes - writtenBytes)

							if (writtenBytes >= targetBytes)
								break
						}
						if (writtenBytes >= targetBytes)
							break
					}
				}
			}
		}
		return out.toByteArray()
	}

	/**
	 * Gets the duration of the audio stream in a video file.
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
	 * Gets the duration of the video file.
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
		return FFmpegFrameGrabber(video).use { grabber ->
			try {
				grabber.start()
				if (width > 0 && height > 0) {
					grabber.imageWidth = width
					grabber.imageHeight = height
				}
				grabber.timestamp = (grabber.lengthInTime * permille) / 1000L
				val frame = grabber.grabImage()
					?: return@use null

				converter.convert(frame)
			} catch (e: Exception) {
				e.printStackTrace()
				null
			}
		}
	}
}