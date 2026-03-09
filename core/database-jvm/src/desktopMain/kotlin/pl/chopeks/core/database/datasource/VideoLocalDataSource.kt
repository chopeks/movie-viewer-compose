package pl.chopeks.core.database.datasource

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
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
			var matchedIdsQuery = MovieTable.select(MovieTable.id)

			if (actors.isNotEmpty()) {
				val actorIds = actors.map { it.id }
				if (actorIds.contains(0)) {
					matchedIdsQuery = matchedIdsQuery.where {
						MovieTable.id notInSubQuery MovieActors.select(MovieActors.movie)
					}
				} else {
					val matches = MovieActors
						.select(MovieActors.movie)
						.where { MovieActors.actor inList actorIds }
						.groupBy(MovieActors.movie)
						.having { MovieActors.movie.count() eq actorIds.size.toLong() }
						.map { it[MovieActors.movie] }

					matchedIdsQuery = matchedIdsQuery.where { MovieTable.id inList matches }
				}
			}

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

			val totalCount = matchedIdsQuery.count()

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