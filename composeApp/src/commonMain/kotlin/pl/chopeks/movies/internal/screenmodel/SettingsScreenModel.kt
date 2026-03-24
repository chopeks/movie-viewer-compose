package pl.chopeks.movies.internal.screenmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.launch
import pl.chopeks.core.data.repository.ISettingsRepository
import pl.chopeks.core.model.Path
import pl.chopeks.core.model.Settings
import pl.chopeks.movies.bestConcurrencyDispatcher

class SettingsScreenModel(
	private val repository: ISettingsRepository,
) : ScreenModel {
	var settings by mutableStateOf<Settings?>(null)
	val pathes = mutableStateListOf<Path>()


	fun init() {
		screenModelScope.launch(bestConcurrencyDispatcher()) {
			settings = repository.getSettings()
			pathes.clear()
			pathes.addAll(repository.getPaths())
		}
	}

	fun saveSettings(browser: String, moviePlayer: String) {
		screenModelScope.launch(bestConcurrencyDispatcher()) {
			repository.setSettings(Settings(browser, moviePlayer))
		}
	}

	fun removePath(path: Path) {
		screenModelScope.launch(bestConcurrencyDispatcher()) {
			repository.removePath(path)
		}
	}

	fun addPath(path: String) {
		screenModelScope.launch(bestConcurrencyDispatcher()) {
			repository.addPath(path)
		}
	}
}