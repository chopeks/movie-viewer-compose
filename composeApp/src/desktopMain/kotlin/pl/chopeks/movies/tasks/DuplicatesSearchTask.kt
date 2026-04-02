package pl.chopeks.movies.tasks

import pl.chopeks.core.data.repository.SystemCapabilityRepository
import pl.chopeks.core.model.capability.FeatureNotSupportedException
import pl.chopeks.movies.tasks.duplicates.CollectFingerprintsUseCase
import pl.chopeks.movies.tasks.duplicates.CompareAudioUseCase
import pl.chopeks.movies.tasks.duplicates.CompareVideoFramesUseCase
import pl.chopeks.movies.utils.AppLogger

class DuplicatesSearchTask(
	private val capabilityRepository: SystemCapabilityRepository,
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

		with(capabilityRepository::contains) {
			try {
				while (true)
					if (!collectFingerprintsUseCase.run())
						break
			} catch (e: FeatureNotSupportedException) {
				AppLogger.log("Collecting fingerprints failed: Feature not supported: ${e.message}")
			} catch (e: Throwable) {
				e.printStackTrace()
				AppLogger.log("Collecting fingerprints failed: ${e.message}")
			}
			try {
				while (true)
					if (!compareAudioUseCase.run())
						break
			} catch (e: FeatureNotSupportedException) {
				AppLogger.log("Audio deduplication failed: Feature not supported: ${e.message}")
			} catch (e: Throwable) {
				e.printStackTrace()
				AppLogger.log("Audio deduplication failed: ${e.message}")
			}
			try {
				while (true)
					if (!compareVideoFramesUseCase.run())
						break
			} catch (e: FeatureNotSupportedException) {
				AppLogger.log("Video deduplication failed: Feature not supported: ${e.message}")
			} catch (e: Throwable) {
				e.printStackTrace()
				AppLogger.log("Video deduplication failed: ${e.message}")
			}
		}

		AppLogger.log("Stopped Duplicates Search Task")
		isRunning = false
	}
}