package pl.chopeks.movies.tasks.duplicates

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import pl.chopeks.core.database.AudioToBeCheckedTable
import pl.chopeks.core.database.DetectedDuplicatesTable
import pl.chopeks.core.database.MovieActors
import pl.chopeks.core.database.MovieTable
import pl.chopeks.movies.server.utils.FpcalcUtils
import pl.chopeks.movies.utils.AppLogger
import java.io.File
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.roundToLong

object CompareAudioUseCase {
	private data class PossibleDuplicate(
		val id: Int,
		val duration: Int,
		val path: File,
		val candidates: List<Int> = emptyList()
	)

	data class MatchResult(
		val id: Int = 0,
		val confidence: Double,
		val timestampSeconds: Double
	)

	/**
	 * @param threshold - will skip vids shorter than this, as these usually are just false positives
	 * @return false if there are no more movies to be checked
	 * @return true if a movie was checked and is ready to check next
	 */
	suspend fun run(threshold: Int = 60 * 1000): Boolean = withContext(Dispatchers.Default) {
		val video = transaction {
			AudioToBeCheckedTable
				.join(MovieTable, JoinType.INNER, onColumn = AudioToBeCheckedTable.id, otherColumn = MovieTable.id) { AudioToBeCheckedTable.id eq MovieTable.id }
				.select(AudioToBeCheckedTable.id, MovieTable.duration, MovieTable.path)
				.where { MovieTable.duration.isNotNull() }
				.orderBy(MovieTable.duration, SortOrder.ASC)
				.limit(1)
				.singleOrNull()
				?.let { PossibleDuplicate(it[AudioToBeCheckedTable.id].value, it[MovieTable.duration]!!, File(it[MovieTable.path])) }
		}

		if (video == null)
			return@withContext false

		if (video.duration < threshold) {
			transaction {
				AudioToBeCheckedTable.deleteWhere { AudioToBeCheckedTable.id eq video.id }
			}
			return@withContext true
		}

		AppLogger.log("checking ${video.path}")

		val dbTask = async {
			val videos = transaction {
				val actors = MovieActors.selectAll().where { MovieActors.movie eq video.id }.map { it[MovieActors.actor] }
				MovieActors.selectAll().where { MovieActors.actor inList actors }.distinct().map { it[MovieActors.id].value }
			}

			if (videos.isEmpty()) {
				transaction { // in case when no actor found, check same directory
					MovieTable.select(MovieTable.id, MovieTable.path, MovieTable.fingerprint)
						.where {
							(MovieTable.path like "${video.path.parentFile.absolutePath}%") and
								(MovieTable.duration greaterEq threshold) and
								(MovieTable.id neq video.id)
						}
						.map { it[MovieTable.id].value to it[MovieTable.fingerprint] }
				}
			} else {
				transaction { // if there are actors, check all videos from all actors and current dir
					MovieTable.select(MovieTable.id, MovieTable.path, MovieTable.duration, MovieTable.fingerprint)
						.where {
							((MovieTable.path like "${video.path.parentFile.absolutePath}%") or (MovieTable.id inList videos)) and
								(MovieTable.duration greaterEq threshold) and
								(MovieTable.id neq video.id)
						}
						.distinct()
						.map { it[MovieTable.id].value to it[MovieTable.fingerprint] }
				}
			}
		}
		val duration = video.duration
		val sampleLen = min(duration, 60_000)
		val middle = duration / 2
		val fragmentStart = (middle - sampleLen / 2).toDouble()

		val fingerprintTask = async {
			FpcalcUtils.getFingerprint(video.path, fragmentStart.toInt(), sampleLen)
		}

		dbTask.start()
		fingerprintTask.start()

		val fingerprints = dbTask.await()
		val currentSample = fingerprintTask.await()

		if (currentSample == null)
			return@withContext false


		val result = performFastSearch(currentSample, fingerprints)

		transaction {
			result.filter { it.confidence in 0.9..0.999 }.forEach { match ->
				AppLogger.log("found duplicate for ${video.id} -> ${match.id} (${match.confidence})")
				DetectedDuplicatesTable.insert {
					it[movie] = video.id
					it[otherMovie] = match.id
				}
			}
			val deleted = AudioToBeCheckedTable.deleteWhere { AudioToBeCheckedTable.id eq video.id }
			if (deleted > 1)
				AudioToBeCheckedTable.insert { it[AudioToBeCheckedTable.id] = video.id }
		}
		println("finished the search for ${video.id}, checked ${result.size} videos")
		return@withContext true
	}

	suspend fun performFastSearch(needle: UIntArray, fingerprints: List<Pair<Int, ByteArray?>>) = withContext(Dispatchers.Default) {
		val sparseNeedle = preProcessNeedle(needle, 3)
		fingerprints.map { (id, blob) ->
			async {
				calculateMatch(sparseNeedle, blob!!).copy(id = id)
			}
		}.awaitAll()
	}

	/**
	 * Hamming distance search over a sliding window
	 */
	private fun calculateMatch(
		sparseNeedle: UIntArray,
		haystackBlob: ByteArray,
		sampleStep: Int = 3
	): MatchResult { // Returns Pair<Confidence, TimestampInSeconds>
		if (haystackBlob.isEmpty())
			return MatchResult(confidence = 0.0, timestampSeconds = 0.0)

		val hLen = haystackBlob.size / 4
		val sLen = sparseNeedle.size
		val needleFullLen = sLen * sampleStep
		if (hLen < needleFullLen)
			return MatchResult(confidence = 0.0, timestampSeconds = 0.0)

		var bestDist = Int.MAX_VALUE
		var bestOffset = 0
		val searchLimit = hLen - needleFullLen

		for (i in 0..searchLimit) {
			var currentDist = 0
			for (j in 0 until sLen) {
				val base = (i + (j * sampleStep)) shl 2
				// Manual conversion of 4 bytes to UInt
				val hValue = ((haystackBlob[base].toInt() and 0xff shl 24) or
					(haystackBlob[base + 1].toInt() and 0xff shl 16) or
					(haystackBlob[base + 2].toInt() and 0xff shl 8) or
					(haystackBlob[base + 3].toInt() and 0xff)).toUInt()

				currentDist += (sparseNeedle[j] xor hValue).countOneBits()
				if (currentDist >= bestDist) break
			}

			if (currentDist < bestDist) {
				bestDist = currentDist
				bestOffset = i
				if (bestDist == 0) break
			}
		}

		val confidence = 1.0 - (bestDist.toDouble() / (32.0 * sLen))
		// Conversion: index * (seconds per frame). Standard is ~0.124s
		val timestamp = bestOffset * 0.124

		return MatchResult(confidence = confidence, timestampSeconds = timestamp)
	}

	fun preProcessNeedle(original: UIntArray, sampleStep: Int = 3): UIntArray {
		val sparseSize = (original.size + sampleStep - 1) / sampleStep
		return UIntArray(sparseSize) { i -> original[i * sampleStep] }
	}
}