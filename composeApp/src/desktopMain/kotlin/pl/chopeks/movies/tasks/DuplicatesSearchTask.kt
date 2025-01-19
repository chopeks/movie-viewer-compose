package pl.chopeks.movies.tasks

import db.DetectedDuplicates
import db.MovieTable
import db.MoviesToBeCheckedTable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import pl.chopeks.movies.BackgroundTasks
import utils.Python

object DuplicatesSearchTask {
  data class PossibleDuplicate(val id: Int, val candidates: List<Int>)

  fun run() {
    BackgroundTasks.scopes.add(CoroutineScope(Dispatchers.IO + BackgroundTasks.duplicatesJob))
    BackgroundTasks.scopes.last().launch(Dispatchers.IO) {
      while(true)
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
    println("checking possible duplicates for ${model.id}")
    val mainPath = transaction { MovieTable.selectAll().where { MovieTable.id eq model.id }.first()[MovieTable.path] }
    for (candidate in model.candidates) {
      val path = transaction { MovieTable.selectAll().where { MovieTable.id eq candidate }.first()[MovieTable.path] }
      val result = Python.compareVideos(mainPath, path) ?: continue
      println("for $candidate $result")
      if (result.isValid) {
        transaction {
          DetectedDuplicates.insert { new ->
            new[DetectedDuplicates.movie] = model.id
            new[DetectedDuplicates.otherMovie] = candidate
          }
        }
        println("added ${model.id} -> $candidate to possible duplicates")
      }
    }
    return transaction { cleanUp(model.id) }
  }

  private fun cleanUp(movieId: Int): Boolean {
    println("Removing with id $movieId")
    MoviesToBeCheckedTable.deleteWhere { MoviesToBeCheckedTable.id eq movieId }
    return true
  }
}