package pl.chopeks.core.ffmpeg.utils

import java.io.File

internal fun ffmpeg() = FFmpegCommandBuilder()

internal class FFmpegCommandBuilder {
	private val args = mutableListOf("ffmpeg", "-hide_banner", "-loglevel", "error")

	fun seek(timestampUs: Long?) = apply {
		if (timestampUs != null) {
			args += listOf("-ss", formatDurationToFfmpegFormat(timestampUs))
		}
	}

	fun duration(timestampUs: Long?) = apply {
		if (timestampUs != null) {
			args += listOf("-t", formatDurationToFfmpegFormat(timestampUs))
		}
	}

	fun input(file: File) = apply { args += listOf("-i", file.absolutePath) }

	fun frames(count: Int) = apply { args += listOf("-vframes", count.toString()) }

	fun scale(w: Int, h: Int) = apply {
		if (w > 0 && h > 0) {
			args += listOf("-vf", "scale=$w:$h")
		}
	}

	fun codec(type: String, name: String) = apply { args += listOf(type, name) }

	fun videoCodec(name: String) = apply { args += listOf("-vcodec", name) }

	fun format(format: String) = apply { args += listOf("-f", format) }

	fun output(file: File) = apply { args += file.absolutePath }

	fun disableVideoStream() = apply { args += "-vn" }

	fun setAudioChannels(amount: Int) = apply { args += listOf("-ac", amount.toString()) }

	fun setAudioSampling(rate: Int) = apply { args += listOf("-ar", rate.toString()) }

	fun setDefaultAudioFormat() = format("s16le")

	fun negativeMapping() = apply { args += "-" }

	fun pipe() = apply { args += "pipe:1" }

	fun version () = apply { args += "-version" }

	fun custom(string: String) = apply { args += string }

	fun build(): List<String> = args.toList()

	internal fun formatDurationToFfmpegFormat(duration: Long): String {
		val hours = duration / 3600000
		val minutes = (duration % 3600000) / 60000
		val seconds = (duration % 60000) / 1000
		val millis = duration % 1000
		return "%02d:%02d:%02d.%03d".format(hours, minutes, seconds, millis)
	}
}