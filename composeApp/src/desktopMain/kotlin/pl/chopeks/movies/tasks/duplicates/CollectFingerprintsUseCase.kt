package pl.chopeks.movies.tasks.duplicates

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import pl.chopeks.core.database.duplicates.FingerprintLocalDataSource
import pl.chopeks.movies.server.utils.FpcalcUtils
import pl.chopeks.movies.server.utils.toByteArray
import pl.chopeks.movies.utils.AppLogger
import java.io.File
import kotlin.math.min
import kotlin.system.measureTimeMillis

class CollectFingerprintsUseCase(
	val dataSource: FingerprintLocalDataSource
) {
	private val diskSemaphore = Semaphore(4)

	/**
	 * @return false if there are no more movies to be checked
	 * @return true if a movie was checked and is ready to check next
	 */
	suspend fun run(): Boolean = withContext(Dispatchers.Default) {
		var entries = dataSource.getVideosWithoutFingerprints(16)
		if (entries.isEmpty()) {
			AppLogger.log("No entries without fingerprint found, aborting...")
			return@withContext false
		}

		val time = measureTimeMillis {
			entries = entries.map {
				async {
					var entry = it
					val file = File(it.path)

					if (it.fingerprint == null) {
						val fingerprint = diskSemaphore.withPermit {
							FpcalcUtils.getFingerprint(file)
						}
						entry = it.copy(fingerprint = fingerprint?.toByteArray())
					}

					if (it.needle == null) {
						val duration = it.duration
						val sampleLen = min(duration, 60_000)
						val middle = duration / 2
						val fragmentStart = (middle - sampleLen / 2).toDouble()

						val needle = diskSemaphore.withPermit {
							FpcalcUtils.getFingerprint(file, fragmentStart.toInt(), sampleLen)
						}
						entry = it.copy(needle = needle?.toByteArray())
					}

					entry
				}
			}.awaitAll()
		}

		if (entries.isEmpty())
			return@withContext false

		dataSource.store(entries)

		val left = dataSource.countTodo()

		AppLogger.log("added fingerprint to ${entries.joinToString { it.id.toString(10) }} - todo: $left (took ${time / 1000}s)")
		return@withContext true
	}
}