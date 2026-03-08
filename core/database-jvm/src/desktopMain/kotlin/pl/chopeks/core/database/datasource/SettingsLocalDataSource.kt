package pl.chopeks.core.database.datasource

import pl.chopeks.core.database.cache.Cache
import pl.chopeks.core.model.Settings

class SettingsLocalDataSource {
	fun get(): Settings {
		return Cache.settings
	}

	fun set(settings: Settings) {
		Cache.settings = settings
	}
}