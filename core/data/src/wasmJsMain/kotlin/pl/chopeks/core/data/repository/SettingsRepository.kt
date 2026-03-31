package pl.chopeks.core.data.repository

import kotlinx.coroutines.flow.Flow
import pl.chopeks.core.data.utils.RpcWrapper
import pl.chopeks.core.model.Path
import pl.chopeks.core.model.Settings

class SettingsRepository(
	private val delegate: ISettingsRepository
) : ISettingsRepository, RpcWrapper {

	override fun getSettings(): Flow<Settings> {
		return delegate.getSettings()
	}

	override suspend fun setSettings(settings: Settings) = rpc {
		delegate.setSettings(settings)
	}

	override suspend fun getPaths(): List<Path> = rpc {
		delegate.getPaths()
	}

	override suspend fun removePath(path: Path) = rpc {
		delegate.removePath(path)
	}

	override suspend fun addPath(path: String) = rpc {
		delegate.addPath(path)
	}
}