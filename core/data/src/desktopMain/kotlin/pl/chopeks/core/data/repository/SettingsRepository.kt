package pl.chopeks.core.data.repository

import pl.chopeks.core.database.datasource.DirectoriesLocalDataSource
import pl.chopeks.core.database.datasource.SettingsLocalDataSource
import pl.chopeks.core.model.Path
import pl.chopeks.core.model.Settings

class SettingsRepository(
	private val settingsDataSource: SettingsLocalDataSource,
	private val directoriesDataSource: DirectoriesLocalDataSource
) : ISettingsRepository {
	override suspend fun getSettings(): Settings {
		return settingsDataSource.get()
	}

	override suspend fun setSettings(settings: Settings) {
		settingsDataSource.set(settings)
	}

	override suspend fun getPaths(): List<Path> {
		return directoriesDataSource.getPathes()
	}

	override suspend fun removePath(path: Path) {
		directoriesDataSource.remove(path)
	}

	override suspend fun addPath(path: String) {
		directoriesDataSource.add(path)
	}

	override fun close() {
		/* no-op */
	}

}