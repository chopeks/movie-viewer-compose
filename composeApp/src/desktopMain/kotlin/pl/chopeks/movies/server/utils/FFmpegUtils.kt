package pl.chopeks.movies.server.utils

import java.io.File
import java.util.concurrent.TimeUnit

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
}

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
  ).runPipeCommand {
    bytes = it.readBytes()
  }
  return bytes
}
