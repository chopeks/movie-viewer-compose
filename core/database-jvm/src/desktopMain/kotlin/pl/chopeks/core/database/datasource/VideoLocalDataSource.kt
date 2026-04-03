package pl.chopeks.core.database.datasource

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import pl.chopeks.core.database.*
import pl.chopeks.core.model.*
import java.io.File
import java.nio.file.Files

class VideoLocalDataSource(
	private val db: Database,
	private val directories: DirectoriesLocalDataSource
) {
	data class NewVideo(
		val name: String,
		val absolutePath: String,
	)

	suspend fun getVideos(from: Long, actors: List<Actor>, categories: List<Category>, filter: Int?, count: Int = 15): VideoContainer = withContext(Dispatchers.IO) {
		transaction(db) {
			val matchedIdsQuery = MovieTable.select(MovieTable.id)

			if (actors.isNotEmpty()) {
				val actorIds = actors.map { it.id }
				if (actorIds.contains(0)) {
					matchedIdsQuery.where {
						MovieTable.id notInSubQuery MovieActors.select(MovieActors.movie)
					}
				} else {
					val matches = MovieActors
						.select(MovieActors.movie)
						.where { MovieActors.actor inList actorIds }
						.groupBy(MovieActors.movie)
						.having { MovieActors.movie.count() eq actorIds.size.toLong() }
						.map { it[MovieActors.movie] }

					matchedIdsQuery.where { MovieTable.id inList matches }
				}
			}

			if (categories.isNotEmpty()) {
				val catIds = categories.map { it.id }

				val categoryCondition: SqlExpressionBuilder.() -> Op<Boolean> = {
					if (catIds.contains(0)) {
						MovieTable.id notInSubQuery MovieCategories.select(MovieCategories.movie)
					} else {
						val matches = MovieCategories
							.select(MovieCategories.movie)
							.where { MovieCategories.category inList catIds }
							.groupBy(MovieCategories.movie)
							.having { MovieCategories.movie.count() eq catIds.size.toLong() }
							.map { it[MovieCategories.movie] }

						MovieTable.id inList matches
					}
				}

				if (matchedIdsQuery.where == null) {
					matchedIdsQuery.where(categoryCondition)
				} else {
					matchedIdsQuery.andWhere(categoryCondition)
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
				.limit(count)
				.offset(start = from * count)
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

	suspend fun getVideoPath(id: Int): String? = withContext(Dispatchers.IO) {
		transaction(db) {
			MovieTable.select(MovieTable.path).where { MovieTable.id eq id }.firstOrNull()?.get(MovieTable.path)
		}
	}

	suspend fun getImage(video: Video): String? = withContext(Dispatchers.IO) {
		transaction(db) {
			MovieTable.selectAll().where { MovieTable.id eq video.id }.firstOrNull()
				?.let { it[MovieTable.thumbnail] }
				?.substringAfter(",")
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

	suspend fun remove(video: Video) = withContext(Dispatchers.IO) {
		val path = transaction(db) {
			val ret = MovieTable.selectAll().where { MovieTable.id eq video.id }.first()[MovieTable.path]
			MovieTable.deleteWhere { MovieTable.id eq video.id }
			MovieActors.deleteWhere { MovieActors.movie eq video.id }
			MovieCategories.deleteWhere { MovieCategories.movie eq video.id }
			DetectedDuplicatesTable.deleteWhere { DetectedDuplicatesTable.movie eq video.id }
			DetectedDuplicatesTable.deleteWhere { DetectedDuplicatesTable.otherMovie eq video.id }
			MoviesToBeCheckedTable.deleteWhere { MoviesToBeCheckedTable.videoId eq video.id }
			AudioToBeCheckedTable.deleteWhere { AudioToBeCheckedTable.videoId eq video.id }
			ret
		}
		val file = File(path)
		if (file.exists())
			file.delete()
	}

	suspend fun setImage(video: Video, img: String?): String? = withContext(Dispatchers.IO) {
		transaction(db) {
			MovieTable.update({ MovieTable.id eq video.id }, body = {
				it[thumbnail] = img
			})
		}
		img
	}

	suspend fun setDuration(videoId: Int, duration: Int) = withContext(Dispatchers.IO) {
		transaction(db) {
			MovieTable.update({ MovieTable.id eq videoId }, body = {
				it[MovieTable.duration] = duration
			})
		}
	}

	suspend fun countVideosInPath(path: String) = withContext(Dispatchers.IO) {
		transaction(db) {
			MovieTable.select(MovieTable.id, MovieTable.path).where { MovieTable.path like "${path}%" }.count()
		}
	}

	suspend fun getFilesInPath(path: String) = withContext(Dispatchers.IO) {
		transaction(db) {
			MovieTable.select(MovieTable.path)
				.where { MovieTable.path like "${path}%" }
				.map { it[MovieTable.path] }
		}
	}

	suspend fun addNewVideos(list: List<NewVideo>) = withContext(Dispatchers.IO) {
		transaction(db) {
			list.forEach {
				val movie = MovieTable.insert { new ->
					new[MovieTable.name] = it.name
					new[MovieTable.path] = it.absolutePath
				}
				// TODO, to be decided what to do about the dedup when added
				MoviesToBeCheckedTable.insert { it[MoviesToBeCheckedTable.videoId] = movie[MovieTable.id].value }
			}
		}
	}

	suspend fun getVideosWithoutThumbnail(): List<Pair<Int, String>> = withContext(Dispatchers.IO) {
		transaction(db) {
			MovieTable
				.select(MovieTable.id, MovieTable.path, MovieTable.thumbnail)
				.where { MovieTable.thumbnail.isNull() }
				.map { it[MovieTable.id].value to it[MovieTable.path] }
		}
	}

	suspend fun getVideosWithoutDuration(): List<Pair<Int, String>> = withContext(Dispatchers.IO) {
		transaction(db) {
			MovieTable
				.select(MovieTable.id, MovieTable.path, MovieTable.duration)
				.where { MovieTable.duration.isNull() or MovieTable.duration.eq(0) }
				.map { it[MovieTable.id].value to it[MovieTable.path] }
		}
	}

	suspend fun moveToDump(video: Video) = withContext(Dispatchers.IO) {
		val file = getVideoPath(video.id)?.let { File(it) }
			?: return@withContext
		val dump = directories.getDumpPath(file)
			?: return@withContext
		val target = File(dump, file.name)
		Files.move(file.toPath(), target.toPath())
		remove(video)
	}
}