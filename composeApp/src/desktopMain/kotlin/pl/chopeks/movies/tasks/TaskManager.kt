package pl.chopeks.movies.tasks

import kotlinx.coroutines.*
import pl.chopeks.movies.server.utils.RefreshUtils

class TaskManager(
	private val duplicatesSearchTask: DuplicatesSearchTask,
) {
	val job = Job()
	val scope = CoroutineScope(Dispatchers.Main + job)

	suspend fun start(onEvent: (String) -> Unit) { // this is kinda fire once and forget
		withContext(Dispatchers.IO + job) {
			RefreshUtils.refresh(onEvent)
			onEvent("New files added.")
		}
		startDedupTask()
	}

	fun startDedupTask() {
		scope.launch(Dispatchers.Default) {
			duplicatesSearchTask.run()
		}
	}

	fun cancel() {
		job.cancel()
		scope.cancel()
	}
}