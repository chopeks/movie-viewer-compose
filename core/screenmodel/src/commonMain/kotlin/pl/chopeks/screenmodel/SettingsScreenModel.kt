package pl.chopeks.screenmodel

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
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

	private val _externalApps = MutableStateFlow<Map<ExternalSoftware, ExternalAppState>>(emptyMap())
	val externalApps = _externalApps.asStateFlow()

	private val _settings = MutableStateFlow<Settings?>(null)
	val settings = _settings.asStateFlow()

	private val _paths = MutableStateFlow<List<Path>>(emptyList())
	val paths = _paths.asStateFlow()

	private val _capabilities = MutableStateFlow<Map<Capability, Boolean>>(emptyMap())
	val capabilities = _capabilities.asStateFlow()

	fun init() {
		screenModelScope.launch {
			_externalApps.emit(ExternalSoftware.entries.associateWith {
				ExternalAppState(it.visibleName, capabilityRepository.getVersion(it))
			})

			_capabilities.emit(Capability.entries.associateWith {
				capabilityRepository.hasCapability(it)
			})

			launch {
				repository.getSettings().collectLatest {
					_settings.emit(it)
				}
			}
			_paths.emit(repository.getPaths())
		}
	}

	fun saveSettings(browser: String, moviePlayer: String, encoderSource: String, encoderSink: String) {
		screenModelScope.launch {
			repository.setSettings(Settings(browser, moviePlayer, encoderSource, encoderSink))
		}
	}

	fun removePath(path: Path) {
		screenModelScope.launch {
			repository.removePath(path)
		}
	}

	fun addPath(path: String) {
		screenModelScope.launch {
			repository.addPath(path)
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