package pl.chopeks.movies.tasks.duplicates

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
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
		val timestampInCurrentVideoSeconds: Double? = null,
		val timestampInCandidateVideoSeconds: Double? = null
	)

	private data class RawMatch(
		val confidence: Double,
		val offsetSeconds: Double
	)

	private val dbSemaphore = Semaphore(128)

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
			AppLogger.log("needle is ${needle?.isEmpty()?.let { "empty == $it" }}")
			dataSource.removeRequest(video.id)
			return@withContext true
		}

		val haystack = dataSource.getFingerprint(video.id)
		if (haystack == null || haystack.isEmpty()) {
			AppLogger.log("haystack is ${haystack?.isEmpty()?.let { "empty == $it" }}")
			dataSource.removeRequest(video.id)
			return@withContext true
		}

		val videos = actorDataSource.findVideosWithSharedActors(video.id)
		val candidates = dataSource.getCandidates(videos, threshold, video)

		val (shorterVideos, longerVideos) = candidates
			.partition { it.duration < video.duration }

		val result = searchThisInOther(needle, longerVideos) +
			searchOtherInThis(haystack, shorterVideos)

		result.filter { it.confidence > 0.9 }
			.distinctBy { it.id }
			.forEach { match ->
				AppLogger.log("found duplicate for ${video.id} -> ${match.id} (${match.confidence})")
				dataSource.addDuplicate(video.id, match.id);
			}
		val deleted = dataSource.removeRequest(video.id)
		if (deleted > 1)
			dataSource.addRequest(video.id)

		println("finished the search for ${video.id}, checked ${result.size} videos")
		return@withContext true
	}

	suspend fun searchOtherInThis(haystack: ByteArray, candidates: List<AudioDedupLocalDataSource.Candidate>) = withContext(Dispatchers.Default) {
		candidates.map { (id, _) ->
			async {
				val needle = dbSemaphore.withPermit {
					dataSource.getNeedle(id)?.toUIntArray()
				} ?: return@async null.also { AppLogger.log("null needle with id=$id") }
				val sparseNeedle = preProcessNeedle(needle, 3)
				val rawMatch = calculateMatch(sparseNeedle, haystack)

				MatchResult(
					id = id,
					confidence = rawMatch.confidence,
					// The candidate (needle) was found inside the current video (haystack)
					timestampInCurrentVideoSeconds = rawMatch.offsetSeconds,
					timestampInCandidateVideoSeconds = 0.0 // Candidate starts at 0 relative to the match
				)
			}
		}.awaitAll().filterNotNull()
	}

	suspend fun searchThisInOther(needle: UIntArray, candidates: List<AudioDedupLocalDataSource.Candidate>) = withContext(Dispatchers.Default) {
		val sparseNeedle = preProcessNeedle(needle, 3)
		candidates.map { (id, _) ->
			async {
				val haystackBlob = dbSemaphore.withPermit {
					dataSource.getFingerprint(id)
				} ?: return@async null.also { AppLogger.log("null fingerprint with id=$id") }

				val rawMatch = calculateMatch(sparseNeedle, haystackBlob)

				MatchResult(
					id = id,
					confidence = rawMatch.confidence,
					// The current video (needle) was found inside the candidate (haystack)
					timestampInCurrentVideoSeconds = 0.0, // Current starts at 0 relative to the match
					timestampInCandidateVideoSeconds = rawMatch.offsetSeconds
				)
			}
		}.awaitAll().filterNotNull()
	}

	/**
	 * Hamming distance search over a sliding window
	 * TODO timestamps are bugged af and I don't have mental capacity to fix it
	 */
	private fun calculateMatch(
		sparseNeedle: UIntArray,
		haystackBlob: ByteArray,
		sampleStep: Int = 3
	): RawMatch {
		if (haystackBlob.isEmpty())
			return RawMatch(0.0, 0.0)

		val hLen = haystackBlob.size / 4
		val sLen = sparseNeedle.size
		val needleFullLen = sLen * sampleStep
		if (hLen < needleFullLen)
			return RawMatch(0.0, 0.0)

		var bestDist = Int.MAX_VALUE
		var bestOffset = 0
		val searchLimit = hLen - needleFullLen

		for (i in 0..searchLimit) {
			var currentDist = 0
			for (j in 0 until sLen) {
				val base = (i + (j * sampleStep)) shl 2
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
		val timestamp = bestOffset * 0.1238

		return RawMatch(confidence = confidence, offsetSeconds = timestamp)
	}

	fun preProcessNeedle(original: UIntArray, sampleStep: Int = 3): UIntArray {
		val sparseSize = (original.size + sampleStep - 1) / sampleStep
		return UIntArray(sparseSize) { i -> original[i * sampleStep] }
	}
}