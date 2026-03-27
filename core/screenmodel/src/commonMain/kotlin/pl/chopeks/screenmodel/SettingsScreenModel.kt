package pl.chopeks.screenmodel

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pl.chopeks.core.data.bestConcurrencyDispatcher
import pl.chopeks.core.data.repository.ISettingsRepository
import pl.chopeks.core.model.Path
import pl.chopeks.core.model.Settings

class SettingsScreenModel(
	private val repository: ISettingsRepository,
) : ScreenModel {
	private val _settings = MutableStateFlow<Settings?>(null)
	val settings = _settings.asStateFlow()

	private val _paths = MutableStateFlow<List<Path>>(emptyList())
	val paths = _paths.asStateFlow()

	fun init() {
		screenModelScope.launch(bestConcurrencyDispatcher()) {
			_settings.emit(repository.getSettings())
			_paths.emit(repository.getPaths())
		}
	}

	fun saveSettings(browser: String, moviePlayer: String, encoderSource: String, encoderSink: String) {
		screenModelScope.launch(bestConcurrencyDispatcher()) {
			repository.setSettings(Settings(browser, moviePlayer, encoderSource, encoderSink))
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