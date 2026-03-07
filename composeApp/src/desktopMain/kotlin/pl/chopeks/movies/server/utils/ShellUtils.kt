package pl.chopeks.movies.server.utils

import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

fun Array<String>.executeCommand(workingDir: File): String? {
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

fun Array<String>.runCommand(workingDir: File) {
  ProcessBuilder(*this)
    .directory(workingDir)
    .redirectOutput(ProcessBuilder.Redirect.INHERIT)
    .redirectError(ProcessBuilder.Redirect.INHERIT)
    .start()
    .waitFor(1, TimeUnit.SECONDS)
}

fun Array<String>.runPipeCommand(callback: (InputStream) -> Unit) {
  val process = ProcessBuilder(*this)
    .redirectError(ProcessBuilder.Redirect.INHERIT)
    .start()
  process.inputStream.use(callback)
  process.waitFor(1, TimeUnit.SECONDS)
}

fun InputStream.pipe(output: OutputStream) {
  thread {
    this.use { inp ->
      output.use { out ->
        inp.copyTo(out)
      }
    }
  }
}