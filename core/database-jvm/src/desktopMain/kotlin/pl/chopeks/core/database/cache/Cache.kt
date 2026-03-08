package pl.chopeks.core.database.cache

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import pl.chopeks.core.model.Settings
import java.io.File
import java.util.*


object Cache {
  private val settingsFile = findCache()
  private var settingsPojo: Settings? = loadSettings()

  private val defaultMoviePlayer = when (OsCheck.operatingSystemType) {
    OsCheck.OSType.Windows -> "explorer"
    OsCheck.OSType.MacOS -> "open"
    OsCheck.OSType.Linux -> "xdg-open"
    else -> "sh"
  }

  private val defaultBrowser = when (OsCheck.operatingSystemType) {
    OsCheck.OSType.Windows -> "explorer"
    OsCheck.OSType.MacOS -> "open"
    OsCheck.OSType.Linux -> "xdg-open"
    else -> "sh"
  }

  private fun loadSettings(): Settings? {
    return if (settingsFile.exists()) {
      val jsonString = settingsFile.readText()
      Json.decodeFromString(jsonString)
    } else {
      null
    }
  }

  private fun saveSettings(settings: Settings) {
    val jsonString = Json.encodeToString(settings)
    settingsFile.writeText(jsonString)
  }

  var moviePlayer: String
    get() = settingsPojo?.moviePlayer ?: defaultMoviePlayer
    set(value) {
      settingsPojo = settingsPojo?.copy(moviePlayer = value) ?: Settings(defaultBrowser, value)
      saveSettings(settingsPojo!!)
    }

  var browser: String
    get() = settingsPojo?.browser ?: defaultBrowser
    set(value) {
      settingsPojo = settingsPojo?.copy(browser = value) ?: Settings(value, defaultMoviePlayer)
      saveSettings(settingsPojo!!)
    }

  var settings: Settings
    get() = settingsPojo ?: Settings(defaultBrowser, defaultMoviePlayer)
    set(value) {
      settingsPojo = value
      saveSettings(value)
    }
}

object OsCheck {
  val operatingSystemType: OSType
    get() {
      val os = System.getProperty("os.name", "generic").lowercase(Locale.ENGLISH)
      return when {
        os.contains("mac") || os.contains("darwin") -> OSType.MacOS
        os.contains("win") -> OSType.Windows
        os.contains("nux") -> OSType.Linux
        else -> OSType.Other
      }
    }

  enum class OSType {
    Windows, MacOS, Linux, Other
  }
}

private fun findCache(): File {
  val dir = File(System.getProperty("user.dir"))
  if (File(dir, "settings.json").exists())
    return File(dir, "settings.json")
  return File(File(dir, ".."), "settings.json")
}