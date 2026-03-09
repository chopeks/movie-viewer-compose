package pl.chopeks.core.database.datasource

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Join
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.Query
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.alias
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.count
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.except
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import pl.chopeks.core.database.*
import pl.chopeks.core.database.cache.Cache
import pl.chopeks.core.model.*
import java.io.File

class VideoLocalDataSource(
	private val db: Database
) {
	suspend fun getVideos(from: Long, actors: List<Actor>, categories: List<Category>, filter: Int?, count: Int = 15): VideoContainer = withContext(Dispatchers.IO) {
		transaction(db) {
			// 1. Build the base query for matching IDs
			// Passing the ID to .select() replaces the old .slice().selectAll()
			var matchedIdsQuery = MovieTable.select(MovieTable.id)

			// Apply Actor Filtering
			if (actors.isNotEmpty()) {
				val actorIds = actors.map { it.id }
				if (actorIds.contains(0)) {
					// "None" actor logic: Find movies NOT in the MovieActors table
					matchedIdsQuery = matchedIdsQuery.where {
						MovieTable.id notInSubQuery MovieActors.select(MovieActors.movie)
					}
				} else {
					// Relational Division: Find movies that have ALL selected actors
					val matches = MovieActors
						.select(MovieActors.movie)
						.where { MovieActors.actor inList actorIds }
						.groupBy(MovieActors.movie)
						.having { MovieActors.movie.count() eq actorIds.size.toLong() }
						.map { it[MovieActors.movie] }

					matchedIdsQuery = matchedIdsQuery.where { MovieTable.id inList matches }
				}
			}

			// Apply Category Filtering
			if (categories.isNotEmpty()) {
				val catIds = categories.map { it.id }
				if (catIds.contains(0)) {
					matchedIdsQuery = matchedIdsQuery.where {
						MovieTable.id notInSubQuery MovieCategories.select(MovieCategories.movie)
					}
				} else {
					val matches = MovieCategories
						.select(MovieCategories.movie)
						.where { MovieCategories.category inList catIds }
						.groupBy(MovieCategories.movie)
						.having { MovieCategories.movie.count() eq catIds.size.toLong() }
						.map { it[MovieCategories.movie] }

					matchedIdsQuery = matchedIdsQuery.where { MovieTable.id inList matches }
				}
			}

			// 2. Get Total Count (Now a lightweight operation)
			val totalCount = matchedIdsQuery.count()

			// 3. Get the specific page of IDs
			val pagedIds = matchedIdsQuery
				.copy()
				.apply {
					when (filter) {
						1 -> orderBy(MovieTable.duration to SortOrder.DESC)
						else -> orderBy(MovieTable.id to SortOrder.DESC)
					}
				}
				.limit(count, offset = from * count)
				.map { it[MovieTable.id].value }

			// 4. Final Fetch for data
			val movies = if (pagedIds.isEmpty()) emptyList() else {
				MovieTable.select(MovieTable.id, MovieTable.name, MovieTable.duration)
					.where { MovieTable.id inList pagedIds }
					.apply {
						when (filter) {
							1 -> orderBy(MovieTable.duration to SortOrder.DESC)
							else -> orderBy(MovieTable.id to SortOrder.DESC)
						}
					}
					.map {
						Video(it[MovieTable.id].value, it[MovieTable.name], it[MovieTable.duration]?.toLong())
					}
			}

			VideoContainer(movies = movies, count = totalCount)
		}
	}

	suspend fun getImage(video: Video): String? = withContext(Dispatchers.IO) {
		transaction(db) {
			MovieTable.selectAll().where { MovieTable.id eq video.id }.firstOrNull()
				?.let { it[MovieTable.thumbnail] }
				?.substringAfter(",")
		}
	}

	suspend fun refreshImage(video: Video): String? = withContext(Dispatchers.IO) {
		transaction(db) {
			return@transaction null
		}
	}

	suspend fun getInfo(video: Video): VideoInfo = withContext(Dispatchers.IO) {
		transaction(db) {
			VideoInfo(
				categories = MovieCategories.selectAll().where { MovieCategories.movie eq video.id }
					.map { it[MovieCategories.category] },
				actors = MovieActors.selectAll().where { MovieActors.movie eq video.id }
					.map { it[MovieActors.actor] })
		}
	}

	suspend fun play(video: Video): Pair<String, File>? = withContext(Dispatchers.IO) {
		transaction(db) {
			MovieTable.select(MovieTable.id, MovieTable.path).where { MovieTable.id eq video.id }.limit(1).firstOrNull()?.let {
				Cache.moviePlayer to File(it[MovieTable.path])
			}
		}
	}

	suspend fun remove(video: Video) = withContext(Dispatchers.IO) {
		val path = transaction(db) {
			val ret = MovieTable.selectAll().where { MovieTable.id eq video.id }.first()[MovieTable.path]
			MovieTable.deleteWhere { MovieTable.id eq video.id }
			MovieActors.deleteWhere { MovieActors.movie eq video.id }
			MovieCategories.deleteWhere { MovieCategories.movie eq video.id }
			DetectedDuplicatesTable.deleteWhere { DetectedDuplicatesTable.movie eq video.id }
			DetectedDuplicatesTable.deleteWhere { DetectedDuplicatesTable.otherMovie eq video.id }
			MoviesToBeCheckedTable.deleteWhere { MoviesToBeCheckedTable.id eq video.id }
			AudioToBeCheckedTable.deleteWhere { AudioToBeCheckedTable.id eq video.id }
			ret
		}
		File(path).delete()
	}
}