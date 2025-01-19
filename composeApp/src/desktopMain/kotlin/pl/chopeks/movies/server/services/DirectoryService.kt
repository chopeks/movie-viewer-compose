package pl.chopeks.movies.server.services

import pl.chopeks.movies.server.db.MovieTable
import pl.chopeks.movies.server.db.PathsTable
import pl.chopeks.movies.server.db.PathsTable.count
import pl.chopeks.movies.server.db.PathsTable.path
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import pl.chopeks.movies.server.model.PathPojo
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import pl.chopeks.movies.server.utils.RefreshUtils
import pl.chopeks.movies.server.utils.getFiles
import java.io.File
import kotlin.concurrent.thread

fun Route.directoryService() {
  get("/directories") {
    call.respond(transaction {
      PathsTable.selectAll().map { PathPojo(it[path], it[count]) }
    })
  }
  post("/directory") {
    kotlin.runCatching { call.receiveNullable<PathPojo>() }.getOrNull()?.let { json ->
      transaction {
        try {
          PathsTable.insert {
            it[path] = json.path
            it[count] = getFiles(File(json.path)).size
          }
        } catch (e: Throwable) {
        }
      }
      call.respond("{}")
    }
    thread { RefreshUtils.refresh {} }
  }
  post("/directory/remove") {
    kotlin.runCatching { call.receiveNullable<PathPojo>() }.getOrNull()?.let { json ->
      transaction {
        PathsTable.deleteWhere { PathsTable.path eq json.path }
        MovieTable.deleteWhere { MovieTable.path like "${json.path}%" }
      }
    }
    call.respond("{}")
  }
}