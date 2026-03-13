package pl.chopeks.core.database.duplicates

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import pl.chopeks.core.database.DetectedDuplicatesTable
import pl.chopeks.core.database.MovieTable
import pl.chopeks.core.database.MoviesToBeCheckedTable
import java.io.File

class VideoDedupLocalDataStorage(
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
			MoviesToBeCheckedTable
				.join(MovieTable, JoinType.INNER, onColumn = MoviesToBeCheckedTable.id, otherColumn = MovieTable.id) { MoviesToBeCheckedTable.id eq MovieTable.id }
				.select(MoviesToBeCheckedTable.id, MovieTable.duration, MovieTable.path)
				.where { MovieTable.duration.isNotNull() }
				.orderBy(MoviesToBeCheckedTable.id, SortOrder.DESC)
				.limit(1)
				.singleOrNull()
				?.let { PossibleDuplicate(it[MoviesToBeCheckedTable.id].value, it[MovieTable.duration]!!, File(it[MovieTable.path])) }
		}
	}

	suspend fun getCandidates(video: PossibleDuplicate, threshold: Int) = withContext(Dispatchers.IO) {
		transaction(db) {
			MovieTable.selectAll()
				.where {
					(MovieTable.id neq video.id) and
						(MovieTable.path like "${video.path.parentFile.absolutePath}%") and
						(MovieTable.duration greaterEq (video.duration - threshold)) and
						(MovieTable.duration lessEq (video.duration + threshold))
				}
		}.map { it[MovieTable.id].value }
	}

	suspend fun addDuplicate(id: Int, candidateId: Int) = withContext(Dispatchers.IO) {
		transaction(db) {
			DetectedDuplicatesTable.upsert { new ->
				new[DetectedDuplicatesTable.movie] = id
				new[DetectedDuplicatesTable.otherMovie] = candidateId
			}
			DetectedDuplicatesTable.upsert { new ->
				new[DetectedDuplicatesTable.otherMovie] = id
				new[DetectedDuplicatesTable.movie] = candidateId
			}
		}
	}

	suspend fun removeDuplicate(id: Int) = withContext(Dispatchers.IO) {
		transaction(db) {
			MoviesToBeCheckedTable.deleteWhere { MoviesToBeCheckedTable.id eq id }
		}
	}

	suspend fun getPath(id: Int) = withContext(Dispatchers.IO) {
		transaction(db) {
			MovieTable.selectAll().where { MovieTable.id eq id }.firstOrNull()?.getOrNull(MovieTable.path)
		}
	}
}