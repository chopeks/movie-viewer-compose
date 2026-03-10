package pl.chopeks.movies.tasks

import pl.chopeks.movies.tasks.duplicates.CollectFingerprintsUseCase
import pl.chopeks.movies.tasks.duplicates.CompareAudioUseCase

class DuplicatesSearchTask(
	private val collectFingerprintsUseCase: CollectFingerprintsUseCase,
	private val compareAudioUseCase: CompareAudioUseCase,
) {
	private var isRunning = false

	suspend fun run() {
		if (isRunning)
			return
		isRunning = true

		while (true)
			if (!collectFingerprintsUseCase.run())
				break
//		while (true)
//			if (!compareAudioUseCase.run())
//				break
		isRunning = false
	}
}