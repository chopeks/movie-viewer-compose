package pl.chopeks.movies.tasks

import kotlinx.coroutines.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import pl.chopeks.movies.BGTasks
import pl.chopeks.movies.server.db.AudioToBeCheckedTable
import pl.chopeks.movies.server.db.DetectedDuplicatesTable
import pl.chopeks.movies.server.db.MovieTable
import pl.chopeks.movies.server.db.MoviesToBeCheckedTable
import pl.chopeks.movies.utils.AppLogger
import pl.chopeks.movies.server.utils.Python
import java.io.File

object DuplicatesSearchTask {
  data class PossibleDuplicate(val id: Int, val candidates: List<Int>)
  var isRunning = false

  fun run() {
    if (isRunning)
      return
    isRunning = true

    BGTasks.scope.launch(Dispatchers.IO) {
      while (true)
        if (!checkNextMovie())
          break
      while (true)
        if (!checkNextAudio())
          break
      isRunning = false
    }
  }

  /**
   * @return false if there are no more movies to be checked
   * @return true if a movie was checked and is ready to check next
   */
  private fun checkNextMovie(threshold: Int = 1500): Boolean {
    val result = transaction {
      val movieWithDuration = MoviesToBeCheckedTable
        .join(MovieTable, JoinType.INNER, onColumn = MoviesToBeCheckedTable.id, otherColumn = MovieTable.id) { MoviesToBeCheckedTable.id eq MovieTable.id }
        .select(MoviesToBeCheckedTable.id, MovieTable.duration, MovieTable.path)
        .where { MovieTable.duration.isNotNull() }
        .orderBy(MoviesToBeCheckedTable.id, SortOrder.DESC)
        .limit(1)
        .singleOrNull()
      if (movieWithDuration == null)
        return@transaction false

      val movieDuration = movieWithDuration[MovieTable.duration]
      if (movieDuration == null)
        return@transaction false

      AppLogger.log("checking video duplicates for ${movieWithDuration[MoviesToBeCheckedTable.id]} (${convertMillisToDuration(movieWithDuration[MovieTable.duration])}) ${movieWithDuration[MovieTable.path]}")

      val movieId = movieWithDuration[MoviesToBeCheckedTable.id].value
      val query = MovieTable
        .selectAll()
        .where {
          (MovieTable.id neq movieId) and
            (MovieTable.duration greaterEq (movieDuration - threshold)) and
            (MovieTable.duration lessEq (movieDuration + threshold))
        }

      val candidates = query.map { it[MovieTable.id].value }

      if (candidates.isEmpty())
        return@transaction cleanUp(movieId)

      AppLogger.log("found ${candidates.size} possible duplicates for ${movieWithDuration[MovieTable.path]}, checking now")

      return@transaction PossibleDuplicate(movieId, candidates)
    }
    if (result is Boolean)
      return result

    if (result is PossibleDuplicate)
      return checkMovie(result)
    return true
  }

  /**
   * @return false if there are no more movies to be checked
   * @return true if a movie was checked and is ready to check next
   */
  private fun checkNextAudio(threshold: Int = 3 * 60 * 1000): Boolean {
    val result = transaction {
      val movieWithDuration = AudioToBeCheckedTable
        .join(MovieTable, JoinType.INNER, onColumn = AudioToBeCheckedTable.id, otherColumn = MovieTable.id) { AudioToBeCheckedTable.id eq MovieTable.id }
        .select(AudioToBeCheckedTable.id, MovieTable.duration, MovieTable.path)
        .where { MovieTable.duration.isNotNull() }
        .orderBy(AudioToBeCheckedTable.id, SortOrder.DESC)
        .limit(1)
        .singleOrNull()
      if (movieWithDuration == null)
        return@transaction false
      val movieDuration = movieWithDuration[MovieTable.duration]
      if (movieDuration == null)
        return@transaction false

      AppLogger.log("checking audio duplicates for ${movieWithDuration[AudioToBeCheckedTable.id]} (${convertMillisToDuration(movieWithDuration[MovieTable.duration])}) ${movieWithDuration[MovieTable.path]}")

      val movieId = movieWithDuration[AudioToBeCheckedTable.id].value
      val movieDir = File(movieWithDuration[MovieTable.path]).parent
      val query = MovieTable
        .selectAll()
        .where {
          (MovieTable.id neq movieId) and
            (MovieTable.path like "$movieDir%") and
            (MovieTable.duration greaterEq (movieDuration - threshold)) and
            (MovieTable.duration lessEq (movieDuration + threshold))
        }

      val candidates = query.map { it[MovieTable.id].value }

      if (candidates.isEmpty())
        return@transaction cleanUpAudio(movieId)

      AppLogger.log("found ${candidates.size} possible duplicates for ${movieWithDuration[MovieTable.path]}, checking now")

      return@transaction PossibleDuplicate(movieId, candidates)
    }
    if (result is Boolean)
      return result

    if (result is PossibleDuplicate)
      return checkAudio(result)
    return true
  }

  @OptIn(DelicateCoroutinesApi::class)
  private fun checkMovie(model: PossibleDuplicate): Boolean {
    val mainPath = transaction { MovieTable.selectAll().where { MovieTable.id eq model.id }.first()[MovieTable.path] }
    if (model.candidates.isNotEmpty()) {
      runBlocking {
        newFixedThreadPoolContext(4, "python").use { pool ->
          model.candidates.map { candidate ->
            async(pool) {
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
    }
    return transaction { cleanUp(model.id) }
  }

  @OptIn(DelicateCoroutinesApi::class)
  private fun checkAudio(model: PossibleDuplicate): Boolean {
    val mainPath = transaction { MovieTable.selectAll().where { MovieTable.id eq model.id }.first()[MovieTable.path] }
    if (model.candidates.isNotEmpty()) {
      runBlocking {
        newFixedThreadPoolContext(8, "python-audio").use { pool ->
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
    }
    return transaction { cleanUpAudio(model.id) }
  }

  private fun cleanUp(movieId: Int): Boolean {
    AppLogger.log("Removing with id $movieId")
    MoviesToBeCheckedTable.deleteWhere { MoviesToBeCheckedTable.id eq movieId }
    return true
  }

  private fun cleanUpAudio(movieId: Int): Boolean {
    AppLogger.log("Removing with id $movieId")
    AudioToBeCheckedTable.deleteWhere { AudioToBeCheckedTable.id eq movieId }
    return true
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