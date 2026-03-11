package pl.chopeks.movies.tasks.duplicates

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import pl.chopeks.core.database.datasource.ActorLocalDataSource
import pl.chopeks.core.database.duplicates.AudioDedupLocalDataSource
import pl.chopeks.movies.server.utils.toUIntArray
import pl.chopeks.movies.utils.AppLogger

class CompareAudioUseCase(
	private val dataSource: AudioDedupLocalDataSource,
	private val actorDataSource: ActorLocalDataSource,
) {
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
		val video = dataSource.nextVideo()
			?: return@withContext false

		if (video.duration < threshold) {
			dataSource.removeRequest(video.id)
			return@withContext true
		}

		AppLogger.log("checking ${video.path}")

		val needle = dataSource.getNeedle(video.id)?.toUIntArray()
		if (needle == null || needle.isEmpty()) {
			AppLogger.log("needle is ${needle?.isEmpty().let { "empty" }}")
			dataSource.removeRequest(video.id)
			return@withContext true
		}

		val actors = actorDataSource.findActorsByVideo(video.id)
		val fingerprints = dataSource.getFingerprints(actors, threshold, video)
			.filter { it.second != null && it.second!!.isNotEmpty() }

		val result = performFastSearch(needle, fingerprints)
		result.filter { it.confidence in 0.9..0.999 }.forEach { match ->
			AppLogger.log("found duplicate for ${video.id} -> ${match.id} (${match.confidence})")
			dataSource.addDuplicate(video.id, match.id);
		}
		val deleted = dataSource.removeRequest(video.id)
		if (deleted > 1)
			dataSource.addRequest(video.id)

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
	 * TODO timestamps are bugged af and I don't have mental capacity to fix it
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