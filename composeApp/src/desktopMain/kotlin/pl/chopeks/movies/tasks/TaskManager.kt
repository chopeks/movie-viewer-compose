package pl.chopeks.movies.tasks

import kotlinx.coroutines.*
import pl.chopeks.movies.utils.AppLogger

class TaskManager(
	private val duplicatesSearchTask: DuplicatesSearchTask,
	private val videoLookupTask: VideoLookupTask,
) {
	val job = Job()
	val scope = CoroutineScope(Dispatchers.Main + job)

	suspend fun start(onEvent: (String) -> Unit) { // this is kinda fire once and forget
		withContext(Dispatchers.IO + job) {
			videoLookupTask.run {
				AppLogger.log(it)
				onEvent(it)
			}
		}
		startDedupTask()
	}

	fun startDedupTask() {
		scope.launch(Dispatchers.Default) {
			duplicatesSearchTask.run()
		}
	}

	fun startRefreshTask() {
		scope.launch(Dispatchers.Default) {
			videoLookupTask.run()
		}
	}

	fun cancel() {
		job.cancel()
		scope.cancel()
	}
}