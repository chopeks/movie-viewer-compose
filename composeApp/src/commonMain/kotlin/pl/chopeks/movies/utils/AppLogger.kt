package pl.chopeks.movies.utils

import androidx.compose.runtime.mutableStateListOf

object AppLogger {
  val logLines = mutableStateListOf<String>()
  private const val MAX_LINES = 2500

  fun log(message: String) {
    logLines.add(message)
    if (logLines.size > MAX_LINES) {
      logLines.removeRange(0, logLines.size - MAX_LINES)
    }
    println(message)
  }
}