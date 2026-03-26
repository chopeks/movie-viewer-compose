package pl.chopeks.movies.tasks

import pl.chopeks.movies.tasks.duplicates.CollectFingerprintsUseCase
import pl.chopeks.movies.tasks.duplicates.CompareAudioUseCase
import pl.chopeks.movies.tasks.duplicates.CompareVideoFramesUseCase
import pl.chopeks.movies.utils.AppLogger

class DuplicatesSearchTask(
	private val collectFingerprintsUseCase: CollectFingerprintsUseCase,
	private val compareAudioUseCase: CompareAudioUseCase,
	private val compareVideoFramesUseCase: CompareVideoFramesUseCase,
) {
	private var isRunning = false

	suspend fun run() {
		if (isRunning)
			return
		isRunning = true

		AppLogger.log("Started Duplicates Search Task")

		while (true)
			if (!collectFingerprintsUseCase.run())
				break
		while (true)
			if (!compareAudioUseCase.run())
				break
		while (true)
			if (!compareVideoFramesUseCase.run())
				break

		AppLogger.log("Stopped Duplicates Search Task")
		isRunning = false
	}
}