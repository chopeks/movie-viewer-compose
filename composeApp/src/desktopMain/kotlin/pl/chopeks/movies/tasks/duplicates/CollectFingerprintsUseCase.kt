package pl.chopeks.movies.tasks.duplicates

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import pl.chopeks.core.database.MovieTable
import pl.chopeks.movies.server.utils.FpcalcUtils
import pl.chopeks.movies.server.utils.toByteArray
import pl.chopeks.movies.utils.AppLogger
import java.io.File
import kotlin.system.measureTimeMillis

object CollectFingerprintsUseCase {
	data class Fingerprint(val id: Int, val path: String, val fingerprint: ByteArray? = null)

	private val diskSemaphore = Semaphore(4)

	/**
	 * @return false if there are no more movies to be checked
	 * @return true if a movie was checked and is ready to check next
	 */
	suspend fun run(): Boolean = withContext(Dispatchers.Default) {
		var entries = withContext(Dispatchers.IO) {
			transaction {
				MovieTable.select(MovieTable.id, MovieTable.fingerprint, MovieTable.path)
					.where { MovieTable.fingerprint.isNull() }
					.limit(16)
					.map { Fingerprint(it[MovieTable.id].value, it[MovieTable.path]) }
			}
		}
		if (entries.isEmpty())
			return@withContext false

		val time = measureTimeMillis {
			entries = entries.map {
				async {
					val file = File(it.path)
					val fingerprint = diskSemaphore.withPermit {
						FpcalcUtils.getFingerprint(file)
					}
					it.copy(fingerprint = fingerprint?.toByteArray())
				}
			}.awaitAll()
		}

		if (entries.isEmpty())
			return@withContext false

		val left = withContext(Dispatchers.IO) {
			transaction {
				entries.forEach { entry ->
					MovieTable.update({ MovieTable.id eq entry.id }) {
						it[MovieTable.fingerprint] = entry.fingerprint
					}
				}
				MovieTable.select(MovieTable.fingerprint).where { MovieTable.fingerprint.isNull() }.count()
			}
		}

		AppLogger.log("added fingerprint to ${entries.joinToString { it.id.toString(10) }} - todo: $left (took ${time/1000}s)")
		return@withContext true
	}

	suspend fun performFastSearch(needle: UIntArray) = withContext(Dispatchers.Default) {
		val sparseNeedle = preProcessNeedle(needle, 3)

		val allHaystacks = withContext(Dispatchers.IO) {
			transaction {
				MovieTable.selectAll().where { MovieTable.fingerprint.isNotNull() }.map { row ->
					row[MovieTable.id] to row[MovieTable.fingerprint] // Pair of ID and ByteArray
				}
			}
		}

		val results = allHaystacks.map { (id, blob) ->
			async {
				val confidence = calculateConfidence(sparseNeedle, blob!!)
				id to confidence
			}
		}.awaitAll()

		val bestMatch = results.maxByOrNull { it.second }
		println("Best Match ID: ${bestMatch?.first} with Confidence: ${bestMatch?.second}")
	}

	/**
	 * Hamming distance search over a sliding window
	 */
	fun calculateConfidence(
		sparseNeedle: UIntArray,
		haystackBlob: ByteArray,
		sampleStep: Int = 3
	): Double {
		val hLen = haystackBlob.size / 4
		val sLen = sparseNeedle.size
		val needleFullLen = sLen * sampleStep

		if (hLen < needleFullLen) return 0.0

		var bestDist = Int.MAX_VALUE
		val searchLimit = hLen - needleFullLen

		for (i in 0..searchLimit) {
			var currentDist = 0

			for (j in 0 until sLen) {
				val base = (i + (j * sampleStep)) shl 2
				val hValue = (
					(haystackBlob[base].toInt() and 0xff shl 24) or
						(haystackBlob[base + 1].toInt() and 0xff shl 16) or
						(haystackBlob[base + 2].toInt() and 0xff shl 8) or
						(haystackBlob[base + 3].toInt() and 0xff)
					).toUInt()
				currentDist += (sparseNeedle[j] xor hValue).countOneBits()

				if (currentDist >= bestDist)
					break
			}

			if (currentDist < bestDist) {
				bestDist = currentDist
				if (bestDist == 0)
					return 1.0
			}
		}
		return 1.0 - (bestDist.toDouble() / (32.0 * sLen))
	}

	fun preProcessNeedle(original: UIntArray, sampleStep: Int = 3): UIntArray {
		val sparseSize = (original.size + sampleStep - 1) / sampleStep
		return UIntArray(sparseSize) { i -> original[i * sampleStep] }
	}
}