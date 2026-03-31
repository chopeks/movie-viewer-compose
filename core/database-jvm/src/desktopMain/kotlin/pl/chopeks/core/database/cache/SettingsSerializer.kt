package pl.chopeks.core.database.cache

import androidx.datastore.core.okio.OkioSerializer
import kotlinx.serialization.json.Json
import okio.BufferedSink
import okio.BufferedSource
import pl.chopeks.core.database.OsCheck
import pl.chopeks.core.model.Settings


object SettingsSerializer : OkioSerializer<Settings> {
	override val defaultValue: Settings = Settings(
		moviePlayer = when (OsCheck.operatingSystemType) {
			OsCheck.OSType.Windows -> "explorer"
			OsCheck.OSType.MacOS -> "open"
			OsCheck.OSType.Linux -> "xdg-open"
			else -> "sh"
		},
		browser = when (OsCheck.operatingSystemType) {
			OsCheck.OSType.Windows -> "explorer"
			OsCheck.OSType.MacOS -> "open"
			OsCheck.OSType.Linux -> "xdg-open"
			else -> "sh"
		}
	)

	override suspend fun readFrom(source: BufferedSource): Settings {
		return try {
			Json.decodeFromString(Settings.serializer(), source.readUtf8())
		} catch (e: Exception) {
			defaultValue
		}
	}

	override suspend fun writeTo(t: Settings, sink: BufferedSink) {
		sink.use {
			it.writeUtf8(Json.encodeToString(Settings.serializer(), t))
		}
	}
}