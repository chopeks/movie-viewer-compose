package pl.chopeks.movies.tasks

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.newFixedThreadPoolContext
import pl.chopeks.movies.BGTasks
import pl.chopeks.movies.tasks.duplicates.CollectFingerprintsUseCase
import pl.chopeks.movies.tasks.duplicates.CompareAudioTrackBruteForceUseCase
import pl.chopeks.movies.tasks.duplicates.CompareAudioUseCase
import pl.chopeks.movies.tasks.duplicates.CompareVideoFramesUseCase

object DuplicatesSearchTask {
	data class PossibleDuplicate(val id: Int, val candidates: List<Int>)

	var isRunning = false

	fun run() {
		if (isRunning)
			return
		isRunning = true

		BGTasks.scope.launch(Dispatchers.Default) {
				while (true)
					if (!CollectFingerprintsUseCase.run())
						break
				while (true)
					if (!CompareAudioUseCase.run())
						break
//				while (true)
//					if (!CompareVideoFramesUseCase.run(this))
//						break
//				while (true)
//					if (!CompareAudioTrackBruteForceUseCase.run(this, pool))
//						break
			isRunning = false
		}
	}
}