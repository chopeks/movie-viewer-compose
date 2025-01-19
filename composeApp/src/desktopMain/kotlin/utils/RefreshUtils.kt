package utils

import db.MovieTable
import db.PathsTable
import io.github.aakira.napier.Napier
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import java.util.*
import javax.imageio.ImageIO

object RefreshUtils {
  data class PossibleDuplicate(val id: Int, val candidates: List<Int>)

  fun refresh(onEvent: (String) -> Unit) {
    transaction {
      PathsTable.selectAll().map { Pair(File(it[PathsTable.path]), it[PathsTable.count]) }
    }.forEach {
      onEvent("Checking ${it.first.absolutePath}.")
      val fileCount = getFiles(it.first).size.toLong()

      val shouldCheckFiles = transaction {
        val dbCount = MovieTable.selectAll().where { MovieTable.path like "${it.first.absolutePath}%" }.count()
        if (dbCount != fileCount) {
          onEvent("File count different, checking which files were removed or added.")
        }
        dbCount != fileCount
      }

      if (shouldCheckFiles) {
        getFiles(it.first).forEach {
          transaction {
            MovieTable.selectAll().where { MovieTable.path eq it.absolutePath }.also { query ->
              if (query.firstOrNull() == null) {
                onEvent("Adding ${it.absolutePath}.")
                MovieTable.insert { new ->
                  new[MovieTable.name] = it.nameWithoutExtension
                  new[MovieTable.path] = it.absolutePath
                }
              }
            }
          }
        }
      }

      transaction {
        mutableListOf<Pair<Int, String>>().apply {
          MovieTable.selectAll().where { MovieTable.thumbnail.isNull() }.forEach { add(Pair(it[MovieTable.id].value, it[MovieTable.path])) }
        }
      }.forEach { movie ->
        val tempDir = File(UUID.randomUUID().toString().substring(0..7)).apply { mkdirs() }
        try {
          makeScreenshot(tempDir, File(movie.second)).also { img ->
            ImageIO.read(img.readBytes().inputStream())
              .normalizeImage()
              .let { ImageIO.write(it, "jpg", img) }
            transaction {
              MovieTable.update({ MovieTable.id eq movie.first }, body = {
                it[MovieTable.thumbnail] = "data:image/jpg;base64," + String(Base64.getMimeEncoder().encode(img.readBytes()))
              })
            }
            img.delete()
          }
          onEvent("Added thumbnail of ${movie.second}.")
        } catch (e: Throwable) {
        }
        tempDir.delete()
      }

      transaction {
        mutableListOf<Pair<Int, String>>().apply {
          MovieTable.selectAll().where { MovieTable.duration.isNull() or MovieTable.duration.eq(0) }.forEach { add(Pair(it[MovieTable.id].value, it[MovieTable.path])) }
        }
      }.forEach { movie ->
        getVideoDuration(File(movie.second)).also { dur ->
          transaction {
            MovieTable.update({ MovieTable.id eq movie.first }, body = {
              it[MovieTable.duration] = dur.toInt()
            })
          }
        }
        onEvent("Added duration of ${movie.second}.")
      }
      onEvent("Directory check completed.")
    }
    return


    transaction {
      // Select all movies with a self-join on MovieTable
      val aliasOther = MovieTable.alias("other")
      // The duration of the "other" movie should be within ±5 seconds of the current movie's duration
      val query = MovieTable
        .join(aliasOther, JoinType.LEFT, onColumn = MovieTable.duration, otherColumn = aliasOther[MovieTable.duration])
        .selectAll().where {
          (MovieTable.duration.minus(5000) lessEq aliasOther[MovieTable.duration]) and (MovieTable.duration.plus(5000) greaterEq aliasOther[MovieTable.duration])
        }

      val movies = query.map { it[MovieTable.id].value to it[aliasOther[MovieTable.id]].value }
        .groupBy({ it.first }, { it.second })
        .map { PossibleDuplicate(it.key, it.value.filter { v -> v != it.key }) }
        .filter { it.candidates.isNotEmpty() }
        .sortedBy { it.candidates.size }

      movies.forEach {
        println("Movie: ${it.id}, Other Movies: ${it.candidates}")
      }
      println("Movie count: ${movies.size}")

      movies.take(10).forEach {
        val movie = MovieTable.selectAll().where { MovieTable.id eq it.id }.first()[MovieTable.path]
        it.candidates.forEach {
          val candidate = transaction { MovieTable.selectAll().where { MovieTable.id eq it }.first()[MovieTable.path] }
          Python.compareVideos(movie, candidate).also {
            Napier.d("movie $movie params $it")
          }
        }
      }
    }
  }


}