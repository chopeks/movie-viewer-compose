package utils

import db.MovieTable
import db.PathsTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import java.util.*
import javax.imageio.ImageIO

object RefreshUtils {
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
  }
}