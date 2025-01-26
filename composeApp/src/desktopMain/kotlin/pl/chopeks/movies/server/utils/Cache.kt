package pl.chopeks.movies.server.utils

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import pl.chopeks.movies.server.model.SettingsPojo
import java.io.File
import java.util.*


object Cache {
  private val settingsFile = findCache()
  private var settingsPojo: SettingsPojo? = loadSettings()

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

  private fun loadSettings(): SettingsPojo? {
    return if (settingsFile.exists()) {
      val jsonString = settingsFile.readText()
      Json.decodeFromString(jsonString)
    } else {
      null
    }
  }

  private fun saveSettings(settings: SettingsPojo) {
    val jsonString = Json.encodeToString(settings)
    settingsFile.writeText(jsonString)
  }

  var moviePlayer: String
    get() = settingsPojo?.moviePlayer ?: defaultMoviePlayer
    set(value) {
      settingsPojo = settingsPojo?.copy(moviePlayer = value) ?: SettingsPojo(defaultBrowser, value)
      saveSettings(settingsPojo!!)
    }

  var browser: String
    get() = settingsPojo?.browser ?: defaultBrowser
    set(value) {
      settingsPojo = settingsPojo?.copy(browser = value) ?: SettingsPojo(value, defaultMoviePlayer)
      saveSettings(settingsPojo!!)
    }

  var settings: SettingsPojo
    get() = settingsPojo ?: SettingsPojo(defaultBrowser, defaultMoviePlayer)
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