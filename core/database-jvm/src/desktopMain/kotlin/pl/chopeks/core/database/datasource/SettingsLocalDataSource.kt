package pl.chopeks.core.database.datasource

import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.okio.OkioStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import okio.FileSystem
import okio.Path.Companion.toPath
import pl.chopeks.core.database.cache.SettingsSerializer
import pl.chopeks.core.model.Settings
import java.io.File

class SettingsLocalDataSource {
	private val dataStore: DataStore<Settings> = DataStoreFactory.create(
		storage = OkioStorage(
			fileSystem = FileSystem.SYSTEM,
			serializer = SettingsSerializer,
			producePath = { findCachePath().toPath() }
		),
		scope = CoroutineScope(Dispatchers.IO)
	)

	fun get(): Flow<Settings> {
		return dataStore.data
	}

	suspend fun set(settings: Settings) {
		dataStore.updateData { settings }
	}

	private fun findCachePath(): String {
		val dir = File(System.getProperty("user.dir"))
		val fileName = "settings.json"

		if (File(dir, fileName).exists())
			return File(dir, fileName).absolutePath
		val parent = File(dir, "..")
		if (File(parent, fileName).exists())
			return File(parent, fileName).absolutePath
		return File(dir, fileName).absolutePath
	}
}