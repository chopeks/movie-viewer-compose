package pl.chopeks.movies.server.services

import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import pl.chopeks.core.database.MovieTable
import pl.chopeks.core.database.PathsTable
import pl.chopeks.core.database.PathsTable.count
import pl.chopeks.core.database.PathsTable.path
import pl.chopeks.core.model.Path
import pl.chopeks.movies.server.utils.RefreshUtils
import pl.chopeks.core.utils.getFiles
import java.io.File
import kotlin.concurrent.thread

fun Route.directoryService() {
	get("/directories") {
		call.respond(transaction {
			PathsTable.selectAll().map { Path(it[path], it[count]) }
		})
	}
	post("/directory") {
		kotlin.runCatching { call.receiveNullable<Path>() }.getOrNull()?.let { json ->
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
		kotlin.runCatching { call.receiveNullable<Path>() }.getOrNull()?.let { json ->
			transaction {
				PathsTable.deleteWhere { PathsTable.path eq json.path }
				MovieTable.deleteWhere { MovieTable.path like "${json.path}%" }
			}
		}
		call.respond("{}")
	}
}