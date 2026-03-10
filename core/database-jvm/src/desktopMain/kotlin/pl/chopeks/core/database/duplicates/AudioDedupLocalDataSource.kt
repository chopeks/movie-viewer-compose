package pl.chopeks.core.database.duplicates

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.or
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

	/**
	 * Returns next video to process or null if there are no requests pending
	 */
	suspend fun nextVideo(): PossibleDuplicate? = withContext(Dispatchers.IO) {
		transaction(db) {
			AudioToBeCheckedTable
				.join(MovieTable, JoinType.INNER, onColumn = AudioToBeCheckedTable.id, otherColumn = MovieTable.id) { AudioToBeCheckedTable.id eq MovieTable.id }
				.select(AudioToBeCheckedTable.id, MovieTable.duration, MovieTable.path)
				.where { MovieTable.duration.isNotNull() }
				.orderBy(MovieTable.duration, SortOrder.ASC)
				.limit(1)
				.singleOrNull()
				?.let { PossibleDuplicate(it[AudioToBeCheckedTable.id].value, it[MovieTable.duration]!!, File(it[MovieTable.path])) }
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
			AudioToBeCheckedTable.insert { it[AudioToBeCheckedTable.id] = id }
		}
	}

	suspend fun removeRequest(id: Int) = withContext(Dispatchers.IO) {
		transaction(db) {
			AudioToBeCheckedTable.deleteWhere { AudioToBeCheckedTable.id eq id }
		}
	}

	suspend fun getVideos(actors: List<Int>, threshold: Int, video: PossibleDuplicate) = withContext(Dispatchers.IO) {
		if (actors.isEmpty()) {
			transaction(db) { // in case when no actor found, check same directory
				MovieTable.select(MovieTable.id, MovieTable.path, MovieTable.fingerprint)
					.where {
						(MovieTable.path like "${video.path.parentFile.absolutePath}%") and
							(MovieTable.duration greaterEq threshold) and
							(MovieTable.id neq video.id)
					}
					.map { it[MovieTable.id].value to it[MovieTable.fingerprint] }
			}
		} else {
			transaction(db) { // if there are actors, check all videos from all actors and current dir
				MovieTable.select(MovieTable.id, MovieTable.path, MovieTable.duration, MovieTable.fingerprint)
					.where {
						((MovieTable.path like "${video.path.parentFile.absolutePath}%") or (MovieTable.id inList actors)) and
							(MovieTable.duration greaterEq threshold) and
							(MovieTable.id neq video.id)
					}
					.distinct()
					.map { it[MovieTable.id].value to it[MovieTable.fingerprint] }
			}
		}
	}
}