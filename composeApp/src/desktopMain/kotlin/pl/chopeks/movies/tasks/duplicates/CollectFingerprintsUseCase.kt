package pl.chopeks.movies.tasks.duplicates

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import pl.chopeks.movies.server.db.MovieTable
import pl.chopeks.movies.server.utils.FpcalcUtils
import pl.chopeks.movies.server.utils.toByteArray
import pl.chopeks.movies.utils.AppLogger
import java.io.File

object CollectFingerprintsUseCase {
	data class Fingerprint(val id: Int, val path: String, val fingerprint: ByteArray? = null)

	/**
	 * @return false if there are no more movies to be checked
	 * @return true if a movie was checked and is ready to check next
	 */
	fun run(scope: CoroutineScope, pool: ExecutorCoroutineDispatcher): Boolean {
		var entries = transaction {
			MovieTable.select(MovieTable.id, MovieTable.fingerprint, MovieTable.path)
				.where { MovieTable.fingerprint.isNull() }
				.limit(16)
				.map { Fingerprint(it[MovieTable.id].value, it[MovieTable.path]) }
		}
		if (entries.isEmpty())
			return false

		runBlocking(scope.coroutineContext) {
			entries = entries.map {
				async(pool) {
					it.copy(fingerprint = FpcalcUtils.getFingerprint(File(it.path))?.toByteArray())
				}
			}.awaitAll()
		}

		if (entries.isEmpty())
			return false

		val left = transaction {
			entries.forEach { entry ->
				MovieTable.update({ MovieTable.id eq entry.id }) {
					it[MovieTable.fingerprint] = entry.fingerprint
				}
			}
			MovieTable.select(MovieTable.fingerprint).where { MovieTable.fingerprint.isNull() }.count()
		}

		AppLogger.log("added fingerprint to ${entries.joinToString { it.id.toString(10) }} - todo: $left")
		return true
	}

	/**
	 * Hamming distance search over a sliding window
	 */
	fun calculateConfidence(
		sparseNeedle: UIntArray,
		haystackBlob: ByteArray
	): Double {
		val sampleStep = 3
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
}