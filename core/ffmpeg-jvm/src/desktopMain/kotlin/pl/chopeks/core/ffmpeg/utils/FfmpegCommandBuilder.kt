package pl.chopeks.core.ffmpeg.utils


internal fun formatDurationToFfmpegFormat(duration: Long): String {
	val duration = duration.coerceIn(0L, Long.MAX_VALUE)
	val hours = duration / 3600000
	val minutes = (duration % 3600000) / 60000
	val seconds = (duration % 60000) / 1000
	val millis = duration % 1000
	return "%02d:%02d:%02d.%03d".format(hours, minutes, seconds, millis)
		.removePrefix("00:")
		.removePrefix("00:")
}
