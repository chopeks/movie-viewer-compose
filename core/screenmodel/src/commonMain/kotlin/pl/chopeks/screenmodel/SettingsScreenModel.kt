package pl.chopeks.screenmodel

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import pl.chopeks.core.data.repository.ISettingsRepository
import pl.chopeks.core.data.repository.ISystemCapabilityRepository
import pl.chopeks.core.data.service.ISystemCapabilityService
import pl.chopeks.core.model.Path
import pl.chopeks.core.model.Settings
import pl.chopeks.core.model.capability.Capability
import pl.chopeks.core.model.capability.ExternalSoftware

class SettingsScreenModel(
	private val repository: ISettingsRepository,
	private val capabilityRepository: ISystemCapabilityRepository,
	private val capabilityService: ISystemCapabilityService,
) : ScreenModel {
	data class ExternalAppState(
		val name: String,
		val version: String?
	)

	data class UiState(
		val externalApps: Map<ExternalSoftware, ExternalAppState> = emptyMap(),
		val settings: Settings? = null,
		val paths: List<Path> = emptyList(),
		val capabilities: Map<Capability, Boolean> = emptyMap()
	)

	private val _externalApps = MutableStateFlow<Map<ExternalSoftware, ExternalAppState>>(emptyMap())
	private val _settings = MutableStateFlow<Settings?>(null)
	private val _paths = MutableStateFlow<List<Path>>(emptyList())
	private val _capabilities = MutableStateFlow<Map<Capability, Boolean>>(emptyMap())

	val uiState = combine(
		_externalApps,
		_settings,
		_paths,
		_capabilities
	) { externalApps, settings, paths, capabilities ->
		UiState(externalApps, settings, paths, capabilities)
	}.stateIn(
		scope = screenModelScope,
		started = SharingStarted.WhileSubscribed(5000),
		initialValue = UiState()
	)

	fun init() {
		screenModelScope.launch {
			launch {
				repository.getSettings().collectLatest {
					_settings.emit(it)
				}
			}
			_externalApps.emit(ExternalSoftware.entries.associateWith {
				ExternalAppState(it.visibleName, capabilityRepository.getVersion(it))
			})
			_capabilities.emit(Capability.entries.associateWith {
				capabilityRepository.hasCapability(it)
			})
			_paths.emit(repository.getPaths())
		}
	}

	fun saveSettings(browser: String, moviePlayer: String, encoderSource: String, encoderSink: String) {
		screenModelScope.launch {
			repository.setSettings(Settings(browser, moviePlayer, encoderSource, encoderSink))
			repository.getSettings().collectLatest {
				_settings.emit(it)
			}
		}
	}

	fun removePath(path: Path) {
		screenModelScope.launch {
			repository.removePath(path)
			_paths.emit(repository.getPaths())
		}
	}

	fun addPath(path: String) {
		screenModelScope.launch {
			repository.addPath(path)
			_paths.emit(repository.getPaths())
		}
	}

	fun refreshApps() {
		screenModelScope.launch {
			capabilityService.discover()
			_capabilities.emit(Capability.entries.associateWith {
				capabilityRepository.hasCapability(it)
			})
		}
	}
}