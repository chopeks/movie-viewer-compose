package pl.chopeks.movies.tasks.duplicates

import kotlinx.coroutines.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import pl.chopeks.core.database.DetectedDuplicatesTable
import pl.chopeks.core.database.MovieTable
import pl.chopeks.core.database.MoviesToBeCheckedTable
import pl.chopeks.movies.server.utils.Python
import pl.chopeks.movies.utils.AppLogger
import java.io.File

object CompareVideoFramesUseCase {
	private data class PossibleDuplicate(val id: Int, val duration: Int, val path: File, val candidates: List<Int> = emptyList())

	/**
	 * @return false if there are no more movies to be checked
	 * @return true if a movie was checked and is ready to check next
	 */
	fun run(scope: CoroutineScope, threshold: Int = 0): Boolean {
		val video = transaction {
			MoviesToBeCheckedTable
				.join(MovieTable, JoinType.INNER, onColumn = MoviesToBeCheckedTable.id, otherColumn = MovieTable.id) { MoviesToBeCheckedTable.id eq MovieTable.id }
				.select(MoviesToBeCheckedTable.id, MovieTable.duration, MovieTable.path)
				.where { MovieTable.duration.isNotNull() }
				.orderBy(MoviesToBeCheckedTable.id, SortOrder.DESC)
				.limit(1)
				.singleOrNull()
				?.let { PossibleDuplicate(it[MoviesToBeCheckedTable.id].value, it[MovieTable.duration]!!, File(it[MovieTable.path])) }
		}

		if (video == null)
			return false

		AppLogger.log("checking video duplicates for ${video.id} (${convertMillisToDuration(video.duration)}) ${video.path.absolutePath}")

		val query = transaction {
			MovieTable.selectAll()
				.where {
					(MovieTable.id neq video.id) and
						(MovieTable.path like "${video.path.parentFile.absolutePath}%") and
						(MovieTable.duration greaterEq (video.duration - threshold)) and
						(MovieTable.duration lessEq (video.duration + threshold))
				}
		}

		val candidates = query.map { it[MovieTable.id].value }

		if (candidates.isEmpty())
			return cleanUp(video.id)

		AppLogger.log("found ${candidates.size} possible duplicates for ${video.path.absolutePath}, checking now")

		return checkMovie(scope, video.copy(candidates = candidates))
	}

	@OptIn(DelicateCoroutinesApi::class)
	private fun checkMovie(scope: CoroutineScope, model: PossibleDuplicate): Boolean {
		val mainPath = transaction { MovieTable.selectAll().where { MovieTable.id eq model.id }.first()[MovieTable.path] }
		if (model.candidates.isNotEmpty()) {
			runBlocking(scope.coroutineContext) {
				model.candidates.map { candidate ->
					async {
						val path = transaction {
							MovieTable.selectAll().where { MovieTable.id eq candidate }
								.firstOrNull()?.getOrNull(MovieTable.path)
						} ?: return@async
						val result = Python.compareVideos(mainPath, path)
						AppLogger.log("for $candidate $result")
						if (result != null) {
							if (result.isValid) {
								transaction {
									DetectedDuplicatesTable.upsert { new ->
										new[DetectedDuplicatesTable.movie] = model.id
										new[DetectedDuplicatesTable.otherMovie] = candidate
									}
								}
								AppLogger.log("added ${model.id} -> $candidate to possible duplicates")
							}
						}
					}
				}.awaitAll()
			}
		}
		return cleanUp(model.id)
	}

	private fun cleanUp(movieId: Int): Boolean = transaction {
		MoviesToBeCheckedTable.deleteWhere { MoviesToBeCheckedTable.id eq movieId }
		return@transaction true
	}

	private fun convertMillisToDuration(millis: Int?): String {
		if (millis == null)
			return ""

		val hours = millis / 3600000
		val minutes = (millis % 3600000) / 60000
		val seconds = (millis % 60000) / 1000

		return buildString {
			if (hours > 0) append("$hours:")
			append("${if (minutes < 10) "0$minutes" else minutes}:")
			append("${if (seconds < 10) "0$seconds" else seconds}")
		}
	}
}