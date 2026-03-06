package pl.chopeks.movies.server.services

import pl.chopeks.movies.server.db.DetectedDuplicatesTable
import pl.chopeks.movies.server.db.MovieTable
import io.ktor.server.response.*
import io.ktor.server.routing.*
import pl.chopeks.movies.server.model.DuplicatePojo
import pl.chopeks.movies.server.model.DuplicatesPojo
import pl.chopeks.movies.server.model.MoviePojo
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.neq
import org.jetbrains.exposed.sql.transactions.transaction
import pl.chopeks.movies.server.db.AudioToBeCheckedTable
import pl.chopeks.movies.server.db.MoviesToBeCheckedTable
import java.io.File

fun Route.duplicatesService() {
  get("/duplicates") {
    call.respond(transaction {
      val joinColumn: Column<*> = MovieTable.duration
      val inner = MovieTable.select(MovieTable.duration, MovieTable.duration.count())
        .groupBy(MovieTable.duration)
        .having { MovieTable.id.count() greater 1 }
        .alias("q1")

      MovieTable.join(inner, JoinType.INNER, onColumn = MovieTable.duration, otherColumn = inner[joinColumn])
        .selectAll()
        .orderBy(MovieTable.duration)
        .groupBy({
          it[MovieTable.duration]
        }, {
          MoviePojo(it[MovieTable.id].value, it[MovieTable.name], it[MovieTable.duration], "%.2f MB".format(File(it[MovieTable.path]).length() / 1024.0 / 1024.0))
        })
        .toList()
        .map {
          mapOf(
            "duration" to it.first,
            "movie" to it.second
          )
        }
    })
  }

  get("/certain_duplicates") {
    call.respond(transaction {
      DetectedDuplicatesTable.deleteWhere {
        (DetectedDuplicatesTable.movie neq DetectedDuplicatesTable.otherMovie) and (DetectedDuplicatesTable.movie lessEq DetectedDuplicatesTable.otherMovie)
      }
      val alias = MovieTable.alias("otherMovieAlias")
      val query = DetectedDuplicatesTable
        .join(MovieTable, JoinType.INNER, onColumn = DetectedDuplicatesTable.movie, otherColumn = MovieTable.id)
        .join(alias, JoinType.INNER, onColumn = DetectedDuplicatesTable.otherMovie, otherColumn = MovieTable.alias("otherMovieAlias")[MovieTable.id])
        .select(
          DetectedDuplicatesTable.movie, DetectedDuplicatesTable.otherMovie,
          MovieTable.id, MovieTable.name, MovieTable.duration, MovieTable.path,
          alias[MovieTable.id],
          alias[MovieTable.name],
          alias[MovieTable.duration],
          alias[MovieTable.path],
        )
        .limit(8)
      query.map { row ->
        DuplicatesPojo(
          listOf(
            DuplicatePojo(row[MovieTable.id].value, row[MovieTable.name], row[MovieTable.duration] ?: 0, "%.2f MB".format(File(row[MovieTable.path]).length() / 1024.0 / 1024.0)),
            DuplicatePojo(row[alias[MovieTable.id]].value, row[alias[MovieTable.name]], row[alias[MovieTable.duration]] ?: 0, "%.2f MB".format(File(row[alias[MovieTable.path]]).length() / 1024.0 / 1024.0))
          )
        )
      }
    })
  }

  get("/duplicates/left") {
    call.respond(transaction {
      mapOf("count" to MoviesToBeCheckedTable.selectAll().count() + AudioToBeCheckedTable.selectAll().count())
    })
  }

  delete("/duplicates/cancel/{id}/{otherId}") {
    val id = call.parameters["id"]?.toIntOrNull()!!
    val otherId = call.parameters["otherId"]?.toIntOrNull()!!
    (id to otherId).also { pair ->
      DetectedDuplicatesTable.deleteWhere {
        (DetectedDuplicatesTable.movie eq pair.first) and (DetectedDuplicatesTable.otherMovie eq pair.second)
      }
      DetectedDuplicatesTable.deleteWhere {
        (DetectedDuplicatesTable.otherMovie eq pair.first) and (DetectedDuplicatesTable.movie eq pair.second)
      }
      call.respond("{}")
    }
  }
}