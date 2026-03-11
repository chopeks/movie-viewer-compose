package pl.chopeks.core

interface ITaskManager {
	suspend fun start(onEvent: (String) -> Unit)
	fun startDedupTask()
	fun startRefreshTask()
	fun cancel()
}