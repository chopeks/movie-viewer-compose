package pl.chopeks.core.database.datasource

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.neq
import org.jetbrains.exposed.sql.transactions.transaction
import pl.chopeks.core.database.*
import pl.chopeks.core.model.Actor
import pl.chopeks.core.model.Duplicates
import pl.chopeks.core.model.Video
import java.io.File

class DuplicateLocalDataSource(
	private val db: Database
) {
	suspend fun getCertainDuplicates(): List<Duplicates> {
		return withContext(Dispatchers.IO) {
			transaction(db) {
				DetectedDuplicatesTable.deleteWhere {
					(DetectedDuplicatesTable.movie neq DetectedDuplicatesTable.otherMovie) and (DetectedDuplicatesTable.movie lessEq DetectedDuplicatesTable.otherMovie)
				}
				val alias = MovieTable.alias("otherMovieAlias")
				val query = DetectedDuplicatesTable
					.join(MovieTable, JoinType.INNER, onColumn = DetectedDuplicatesTable.movie, otherColumn = MovieTable.id)
					.join(alias, JoinType.INNER, onColumn = DetectedDuplicatesTable.otherMovie, otherColumn = MovieTable.alias("otherMovieAlias")[MovieTable.id])
					.select(
						DetectedDuplicatesTable.movie, DetectedDuplicatesTable.otherMovie,
						DetectedDuplicatesTable.timestamp, DetectedDuplicatesTable.otherTimestamp,
						MovieTable.id, MovieTable.name, MovieTable.duration, MovieTable.path,
						alias[MovieTable.id],
						alias[MovieTable.name],
						alias[MovieTable.duration],
						alias[MovieTable.path],
					)
					.limit(8)
				query.map { row ->
					Duplicates(
						list = listOf(
							Video(row[MovieTable.id].value, row[MovieTable.name], row[MovieTable.duration]?.toLong() ?: 0L, "%.2f MB".format(File(row[MovieTable.path]).length() / 1024.0 / 1024.0)),
							Video(row[alias[MovieTable.id]].value, row[alias[MovieTable.name]], row[alias[MovieTable.duration]]?.toLong() ?: 0L, "%.2f MB".format(File(row[alias[MovieTable.path]]).length() / 1024.0 / 1024.0))
						),
						timestamp = row[DetectedDuplicatesTable.timestamp],
						otherTimestamp = row[DetectedDuplicatesTable.otherTimestamp]
					)
				}
			}
		}
	}

	fun cancel(model: Duplicates) = transaction(db) {
		if (model.list.size < 2)
			return@transaction
		val id = model.list.first().id
		val otherId = model.list.last().id
		(id to otherId).also { pair ->
			transaction {
				DetectedDuplicatesTable.deleteWhere {
					(DetectedDuplicatesTable.movie eq pair.first) and (DetectedDuplicatesTable.otherMovie eq pair.second)
				}
				DetectedDuplicatesTable.deleteWhere {
					(DetectedDuplicatesTable.otherMovie eq pair.first) and (DetectedDuplicatesTable.movie eq pair.second)
				}
			}
		}
	}

	fun count(): Int {
		return transaction { (MoviesToBeCheckedTable.selectAll().count() + AudioToBeCheckedTable.selectAll().count()).toInt() }
	}


	suspend fun deduplicate(actor: Actor) = withContext(Dispatchers.IO) {
		transaction(db) {
			MovieActors
				.select(MovieActors.movie, MovieActors.actor)
				.where { MovieActors.actor eq actor.id }
				.forEach { video ->
					AudioToBeCheckedTable.upsert { it[AudioToBeCheckedTable.id] = video[MovieActors.movie] }
				}
		}
	}

	suspend fun deduplicateAll() = withContext(Dispatchers.IO) {
		transaction(db) {
			AudioToBeCheckedTable.deleteAll()
			MovieTable.select(MovieTable.id).distinct().forEach { video ->
				AudioToBeCheckedTable.upsert { it[AudioToBeCheckedTable.id] = video[MovieTable.id] }
			}
		}
	}
}