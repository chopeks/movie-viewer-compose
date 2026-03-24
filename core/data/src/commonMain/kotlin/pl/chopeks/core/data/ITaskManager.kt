package pl.chopeks.core.data

import kotlinx.rpc.annotations.Rpc

@Rpc
interface ITaskManager {
	suspend fun start(onEvent: (String) -> Unit)
	suspend fun startDedupTask()
	suspend fun startRefreshTask()
}