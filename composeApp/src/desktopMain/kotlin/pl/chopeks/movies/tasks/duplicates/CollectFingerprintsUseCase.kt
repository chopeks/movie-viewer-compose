package pl.chopeks.movies.tasks.duplicates

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import pl.chopeks.core.database.duplicates.FingerprintLocalDataSource
import pl.chopeks.core.fpcalc.FpcalcManager
import pl.chopeks.core.model.capability.CapabilityGuard
import pl.chopeks.movies.server.utils.toByteArray
import pl.chopeks.movies.utils.AppLogger
import java.io.File
import kotlin.math.min
import kotlin.system.measureTimeMillis

class CollectFingerprintsUseCase(
	private val dataSource: FingerprintLocalDataSource,
	private val fpcalc: FpcalcManager
) {
	companion object {
		private val SILENCE_BYTES = byteArrayOf(0x25.toByte(), 0x6D.toByte(), 0xF9.toByte(), 0x77.toByte())
	}

	private val diskSemaphore = Semaphore(8)

	/**
	 * @return false if there are no more movies to be checked
	 * @return true if a movie was checked and is ready to check next
	 */
	@OptIn(ExperimentalUnsignedTypes::class)
	context(guard: CapabilityGuard)
	suspend fun run(): Boolean = withContext(Dispatchers.Default) {
		var entries = dataSource.getVideosWithoutFingerprints(16)
		if (entries.isEmpty()) {
			AppLogger.log("No entries without fingerprint found, aborting...")
			return@withContext false
		}
		val time = measureTimeMillis {
			entries = coroutineScope {
				entries.map {
					async {
						var entry = it
						val file = File(it.path)

						if (it.fingerprint == null) {
							val fingerprint = diskSemaphore.withPermit {
								fpcalc.getFingerprint(file)
							}?.toByteArray() ?: byteArrayOf()

							entry = if (isSilent(fingerprint)) {
								it.copy(fingerprint = byteArrayOf(), needle = byteArrayOf())
							} else {
								it.copy(fingerprint = fingerprint)
							}
						}

						if (it.needle == null && entry.needle == null) {
							val duration = it.duration
							val sampleLen = min(duration, 60_000)
							val middle = duration / 2
							val fragmentStart = (middle - sampleLen / 2).toDouble()

							val needle = diskSemaphore.withPermit {
								fpcalc.getFingerprint(file, fragmentStart.toInt(), sampleLen)
							}?.toByteArray() ?: byteArrayOf()
							entry = if (isSilent(needle)) {
								it.copy(needle = byteArrayOf())
							} else {
								it.copy(needle = needle)
							}
						}

						entry
					}
				}.awaitAll()
			}
		}

		if (entries.isEmpty())
			return@withContext false

		dataSource.store(entries)

		val left = dataSource.countTodo()

		AppLogger.log("added fingerprint to ${entries.joinToString { it.id.toString(10) }} - todo: $left (took ${time / 1000}s)")
		return@withContext true
	}

	fun isSilent(blob: ByteArray, threshold: Double = 0.9): Boolean {
		if (blob.size < 4)
			return true

		val frameCount = blob.size / 4
		var silenceCount = 0

		for (i in 0 until frameCount) {
			val base = i shl 2 // same as i * 4

			// Compare 4 bytes at once
			val isSilence = blob[base] == SILENCE_BYTES[0] &&
				blob[base + 1] == SILENCE_BYTES[1] &&
				blob[base + 2] == SILENCE_BYTES[2] &&
				blob[base + 3] == SILENCE_BYTES[3]

			if (isSilence)
				silenceCount++
		}

		val ratio = silenceCount.toDouble() / frameCount
		return ratio >= threshold
	}
}