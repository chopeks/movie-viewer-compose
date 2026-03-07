package pl.chopeks.movies.tasks.duplicates

import kotlinx.coroutines.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import pl.chopeks.movies.server.db.AudioToBeCheckedTable
import pl.chopeks.movies.server.db.DetectedDuplicatesTable
import pl.chopeks.movies.server.db.MovieTable
import pl.chopeks.movies.server.utils.Python
import pl.chopeks.movies.utils.AppLogger
import java.io.File

object CompareAudioTrackBruteForceUseCase {
	private data class PossibleDuplicate(val id: Int, val duration: Int, val path: File, val candidates: List<Int> = emptyList())

	/**
	 * @return false if there are no more movies to be checked
	 * @return true if a movie was checked and is ready to check next
	 */
	fun run(scope: CoroutineScope, pool: ExecutorCoroutineDispatcher, threshold: Int = 3 * 60 * 1000): Boolean {
		val video = transaction {
			AudioToBeCheckedTable
				.join(MovieTable, JoinType.INNER, onColumn = AudioToBeCheckedTable.id, otherColumn = MovieTable.id) { AudioToBeCheckedTable.id eq MovieTable.id }
				.select(AudioToBeCheckedTable.id, MovieTable.duration, MovieTable.path)
				.where { MovieTable.duration.isNotNull() }
				.orderBy(AudioToBeCheckedTable.id, SortOrder.DESC)
				.limit(1)
				.singleOrNull()
				?.let { PossibleDuplicate(it[AudioToBeCheckedTable.id].value, it[MovieTable.duration]!!, File(it[MovieTable.path])) }
		}
		if (video == null)
			return false

		AppLogger.log("checking audio duplicates for ${video.id} (${convertMillisToDuration(video.duration)}) ${video.path.absolutePath}")

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

		return checkAudio(scope, pool, video.copy(candidates = candidates))
	}


	@OptIn(DelicateCoroutinesApi::class)
	private fun checkAudio(scope: CoroutineScope, pool: ExecutorCoroutineDispatcher, model: PossibleDuplicate): Boolean {
		val mainPath = transaction { MovieTable.selectAll().where { MovieTable.id eq model.id }.first()[MovieTable.path] }
		if (model.candidates.isNotEmpty()) {
			runBlocking(scope.coroutineContext) {
				model.candidates.map { candidate ->
					async(pool) {
						val path = transaction {
							MovieTable.selectAll().where { MovieTable.id eq candidate }
								.firstOrNull()?.getOrNull(MovieTable.path)
						} ?: return@async
						val result = Python.compareAudios(mainPath, path)
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
		AppLogger.log("Removing with id $movieId")
		AudioToBeCheckedTable.deleteWhere { AudioToBeCheckedTable.id eq movieId }
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