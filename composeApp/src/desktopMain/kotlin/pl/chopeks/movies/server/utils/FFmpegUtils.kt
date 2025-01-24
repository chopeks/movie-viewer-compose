package pl.chopeks.movies.server.utils

import java.io.File
import java.util.concurrent.TimeUnit

fun getVideoDuration(video: File) = try {
  arrayOf("ffprobe", "-show_entries", "format=duration", "-sexagesimal", "\"${video.absolutePath}\"")
    .executeCommand(File("./"))
    ?.lines()?.filter { "duration" in it }?.get(0)?.split("=")?.get(1)
    ?.let {
      val parts = it.replace(".", ":").split(":").map { it.toLong() }
      TimeUnit.HOURS.toMillis(parts[0]) + TimeUnit.MINUTES.toMillis(parts[1]) + TimeUnit.SECONDS.toMillis(parts[2]) + TimeUnit.NANOSECONDS.toMillis(parts[3])
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


fun makeScreenshots(targetDir: File, video: File): Array<File> {
  val interval = getVideoDuration(video) / 11L
  for (i in 1..9) {
    arrayOf(
      "ffmpeg",
      "-ss",
      "${TimeUnit.MILLISECONDS.toHours(i * interval)}:${TimeUnit.MILLISECONDS.toMinutes(i * interval) % 60}:${TimeUnit.MILLISECONDS.toSeconds(i * interval) % 60}",
      "-i",
      "\"${video.absolutePath}\"",
      "-vframes",
      "1",
      "${i}.jpg"
    ).runCommand(targetDir)
  }
  return targetDir.listFiles()
}
