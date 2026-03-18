package com.chopeks.pl.chopeks.core.ffmpeg

import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.TimeUnit

class FfmpegManager {
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