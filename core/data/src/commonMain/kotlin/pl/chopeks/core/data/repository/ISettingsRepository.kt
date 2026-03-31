package pl.chopeks.core.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.rpc.annotations.Rpc
import pl.chopeks.core.model.Path
import pl.chopeks.core.model.Settings

@Rpc
interface ISettingsRepository{
	fun getSettings(): Flow<Settings>
	suspend fun setSettings(settings: Settings)
	suspend fun getPaths(): List<Path>
	suspend fun removePath(path: Path)
	suspend fun addPath(path: String)
}