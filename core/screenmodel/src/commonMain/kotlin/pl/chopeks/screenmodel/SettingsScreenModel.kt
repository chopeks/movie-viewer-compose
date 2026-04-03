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

	sealed class Intent {
		data object Init : Intent()
		data class SaveSettings(
			val browser: String,
			val moviePlayer: String,
			val encoderSource: String,
			val encoderSink: String
		) : Intent()

		data class RemovePath(val path: Path) : Intent()
		data class AddPath(val path: String) : Intent()
		data object RefreshApps : Intent()
	}

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

	private val _state = MutableStateFlow(UiState())
	val state: StateFlow<UiState> = _state.asStateFlow()

	fun handleIntent(intent: Intent) {
		when (intent) {
			is Intent.Init -> init()
			is Intent.SaveSettings -> saveSettings(
				intent.browser,
				intent.moviePlayer,
				intent.encoderSource,
				intent.encoderSink
			)

			is Intent.RemovePath -> removePath(intent.path)
			is Intent.AddPath -> addPath(intent.path)
			is Intent.RefreshApps -> refreshApps()
		}
	}

	private fun init() {
		screenModelScope.launch {
			launch {
				repository.getSettings().collectLatest { settings ->
					_state.update { it.copy(settings = settings) }
				}
			}
			_state.update {
				it.copy(
					externalApps = ExternalSoftware.entries.associateWith {
						ExternalAppState(it.visibleName, capabilityRepository.getVersion(it))
					},
					capabilities = Capability.entries.associateWith {
						capabilityRepository.hasCapability(it)
					},
					paths = repository.getPaths()
				)
			}
		}
	}

	private fun saveSettings(
		browser: String,
		moviePlayer: String,
		encoderSource: String,
		encoderSink: String
	) {
		screenModelScope.launch {
			repository.setSettings(Settings(browser, moviePlayer, encoderSource, encoderSink))
			repository.getSettings().collectLatest { settings ->
				_state.update { it.copy(settings = settings) }
			}
		}
	}

	private fun removePath(path: Path) {
		screenModelScope.launch {
			repository.removePath(path)
			val paths = repository.getPaths()
			_state.update { it.copy(paths = paths) }
		}
	}

	private fun addPath(path: String) {
		screenModelScope.launch {
			repository.addPath(path)
			val paths = repository.getPaths()
			_state.update { it.copy(paths = paths) }
		}
	}

	private fun refreshApps() {
		screenModelScope.launch {
			capabilityService.discover()
			val capabilities = Capability.entries.associateWith {
				capabilityRepository.hasCapability(it)
			}
			_state.update { it.copy(capabilities = capabilities) }
		}
	}
}
