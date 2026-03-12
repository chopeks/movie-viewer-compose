package pl.chopeks.core.database.duplicates

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import pl.chopeks.core.database.AudioToBeCheckedTable
import pl.chopeks.core.database.DetectedDuplicatesTable
import pl.chopeks.core.database.MovieTable
import java.io.File

class AudioDedupLocalDataSource(
	private val db: Database
) {
	data class PossibleDuplicate(
		val id: Int,
		val duration: Int,
		val path: File,
		val candidates: List<Int> = emptyList()
	)

	data class Candidate(
		val id: Int,
		val duration: Int
	)

	/**
	 * Returns next video to process or null if there are no requests pending
	 */
	suspend fun nextVideo(): PossibleDuplicate? = withContext(Dispatchers.IO) {
		transaction(db) {
			AudioToBeCheckedTable
				.join(MovieTable, JoinType.INNER, onColumn = AudioToBeCheckedTable.videoId, otherColumn = MovieTable.id) { AudioToBeCheckedTable.videoId eq MovieTable.id }
				.select(AudioToBeCheckedTable.videoId, MovieTable.duration, MovieTable.path)
				.where { MovieTable.duration.isNotNull() }
				.orderBy(MovieTable.id, SortOrder.DESC)
				.limit(1)
				.singleOrNull()
				?.let { PossibleDuplicate(it[AudioToBeCheckedTable.videoId], it[MovieTable.duration]!!, File(it[MovieTable.path])) }
		}
	}

	suspend fun addDuplicate(videoId: Int, matchId: Int) = withContext(Dispatchers.IO) {
		transaction(db) {
			DetectedDuplicatesTable.insert {
				it[movie] = videoId
				it[otherMovie] = matchId
			}
			DetectedDuplicatesTable.insert {
				it[movie] = matchId
				it[otherMovie] = videoId
			}
		}
	}

	suspend fun addRequest(id: Int) = withContext(Dispatchers.IO) {
		transaction(db) {
			AudioToBeCheckedTable.insert { it[AudioToBeCheckedTable.videoId] = id }
		}
	}

	suspend fun removeRequest(id: Int) = withContext(Dispatchers.IO) {
		transaction(db) {
			AudioToBeCheckedTable.deleteWhere { AudioToBeCheckedTable.videoId eq id }
		}
	}

	suspend fun getCandidates(videos: List<Int>, threshold: Int, video: PossibleDuplicate): List<Candidate> = withContext(Dispatchers.IO) {
		transaction(db) {
			MovieTable.select(MovieTable.id, MovieTable.path, MovieTable.duration)
				.where {
					((MovieTable.path like "${video.path.parentFile.absolutePath}%") or (MovieTable.id inList videos)) and
						(MovieTable.duration greaterEq threshold) and
						(MovieTable.id neq video.id)
				}
				.distinct()
				.map {
					Candidate(
						it[MovieTable.id].value,
						it[MovieTable.duration]!!
					)
				}
		}
	}

	suspend fun getNeedle(videoId: Int) = withContext(Dispatchers.IO) {
		transaction(db) {
			MovieTable.select(MovieTable.id, MovieTable.needle)
				.where { MovieTable.id eq videoId }
				.limit(1)
				.map { it[MovieTable.needle] }
				.firstOrNull()
		}
	}

	suspend fun getFingerprint(videoId: Int) = withContext(Dispatchers.IO) {
		transaction(db) {
			MovieTable.select(MovieTable.id, MovieTable.fingerprint)
				.where { MovieTable.id eq videoId }
				.limit(1)
				.map { it[MovieTable.fingerprint] }
				.firstOrNull()
		}
	}

	suspend fun reset() = withContext(Dispatchers.IO) {
		transaction(db) {
			DetectedDuplicatesTable.deleteAll()

			AudioToBeCheckedTable.deleteAll()
			MovieTable.select(MovieTable.id).orderBy(MovieTable.id, SortOrder.DESC).map { it[MovieTable.id] }.forEach { movieId ->
				AudioToBeCheckedTable.insert { it[AudioToBeCheckedTable.videoId] = movieId.value }
			}

//			MoviesToBeCheckedTable.deleteAll()
//			MovieTable.select(MovieTable.id).orderBy(MovieTable.id, SortOrder.DESC).take(5).map { it[MovieTable.id] }.forEach { movieId ->
//				MoviesToBeCheckedTable.insert { it[MoviesToBeCheckedTable.id] = movieId }
//			}
		}
	}
}