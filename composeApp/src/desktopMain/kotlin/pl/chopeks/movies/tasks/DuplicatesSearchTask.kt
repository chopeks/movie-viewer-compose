package pl.chopeks.movies.tasks

import kotlinx.coroutines.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import pl.chopeks.movies.BGTasks
import pl.chopeks.movies.server.db.DetectedDuplicatesTable
import pl.chopeks.movies.server.db.MovieTable
import pl.chopeks.movies.server.db.MoviesToBeCheckedTable
import pl.chopeks.movies.server.utils.Python

object DuplicatesSearchTask {
  data class PossibleDuplicate(val id: Int, val candidates: List<Int>)

  fun run() {
    BGTasks.scope.launch(Dispatchers.IO) {
      while (true)
        if (!checkNextMovie())
          break
    }
  }

  /**
   * @return false if there are no more movies to be checked
   * @return true if a movie was checked and is ready to check next
   */
  private fun checkNextMovie(threshold: Int = 5000): Boolean {
    val result = transaction {
      val movieWithDuration = MoviesToBeCheckedTable
        .join(MovieTable, JoinType.INNER, onColumn = MoviesToBeCheckedTable.id, otherColumn = MovieTable.id) { MoviesToBeCheckedTable.id eq MovieTable.id }
        .select(MoviesToBeCheckedTable.id, MovieTable.duration)
        .where { MovieTable.duration.isNotNull() }
        .orderBy(MovieTable.duration, SortOrder.ASC)
        .limit(1)
        .singleOrNull()
      if (movieWithDuration == null)
        return@transaction false
      println("checking possible duplicates for ${movieWithDuration[MovieTable.id]} (${convertMillisToDuration(movieWithDuration[MovieTable.duration])})")
      // query movies, that have duration within threshold
      val movieId = movieWithDuration[MoviesToBeCheckedTable.id].value
      val aliasOther = MovieTable.select(MovieTable.id, MovieTable.duration).alias("other")
      val query = MovieTable
        .join(aliasOther, JoinType.LEFT, onColumn = MovieTable.duration, otherColumn = aliasOther[MovieTable.duration])
        .selectAll()
        .where {
          (MovieTable.id eq movieId) and (aliasOther[MovieTable.id] neq movieId) and
            (MovieTable.duration.minus(threshold) lessEq aliasOther[MovieTable.duration]) and
            (MovieTable.duration.plus(threshold) greaterEq aliasOther[MovieTable.duration])
        }

      if (query.count() == 0L)
        return@transaction cleanUp(movieId)

      val possibleDuplicates = query
        .map { it[MovieTable.id].value to it[aliasOther[MovieTable.id]].value }
        .groupBy({ it.first }, { it.second })
        .map { PossibleDuplicate(it.key, it.value.filter { v -> v != it.key }) }
        .firstOrNull { it.id == movieId }

      if (possibleDuplicates == null)
        return@transaction cleanUp(movieId)
      return@transaction possibleDuplicates
    }
    if (result is Boolean)
      return result

    if (result is PossibleDuplicate)
      return checkMovie(result)
    return true
  }

  private fun checkMovie(model: PossibleDuplicate): Boolean {
    val mainPath = transaction { MovieTable.selectAll().where { MovieTable.id eq model.id }.first()[MovieTable.path] }
    runBlocking {
      model.candidates.map { candidate ->
        async(Dispatchers.IO) {
          val path = transaction { MovieTable.selectAll().where { MovieTable.id eq candidate }.first()[MovieTable.path] }
          val result = Python.compareVideos(mainPath, path)
          println("for $candidate $result")
          if (result != null) {
            if (result.isValid) {
              transaction {
                DetectedDuplicatesTable.insert { new ->
                  new[DetectedDuplicatesTable.movie] = model.id
                  new[DetectedDuplicatesTable.otherMovie] = candidate
                }
              }
              println("added ${model.id} -> $candidate to possible duplicates")
            }
          }
        }
      }.awaitAll()
    }
    return transaction { cleanUp(model.id) }
  }

  private fun cleanUp(movieId: Int): Boolean {
    println("Removing with id $movieId")
    MoviesToBeCheckedTable.deleteWhere { MoviesToBeCheckedTable.id eq movieId }
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