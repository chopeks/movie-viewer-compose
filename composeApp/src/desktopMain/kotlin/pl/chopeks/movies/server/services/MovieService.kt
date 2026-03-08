package pl.chopeks.movies.server.services

import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import pl.chopeks.core.database.*
import pl.chopeks.core.database.cache.Cache
import pl.chopeks.movies.server.model.MoviePojo
import pl.chopeks.movies.server.utils.runCommand
import java.io.File

fun Route.movieService() {
	get("/movie/play/{id}") {
		transaction {
			MovieTable.selectAll().where { MovieTable.id eq (call.parameters["id"]!!.toInt()) }.limit(1).firstOrNull().also {
				if (it != null) {
					arrayOf(Cache.moviePlayer, "\"${it[MovieTable.path]}\"").runCommand(File(it[MovieTable.path]).parentFile)
				}
			}
		}
		call.respond(HttpStatusCode.OK)
	}
	get("/movie/{from}/{count}") {
		call.respond(transaction {
			val categories = call.request.queryParameters["category"]
			val actors = call.request.queryParameters["actor"]

			var columnCategory: Column<*> = MovieTable.id
			val viaCategory = when (categories) {
				null, "null" -> MovieTable.select(MovieTable.id)
				"0" -> MovieTable
					.join(MovieCategories, JoinType.LEFT, MovieTable.id, MovieCategories.movie)
					.select(MovieTable.id)
					.where { MovieCategories.category.isNull() }

				else -> categories.split(",").map { it.toInt() }.let {
					columnCategory = MovieCategories.movie
					MovieCategories
						.select(MovieCategories.movie)
						.where { MovieCategories.category inList it }
						.groupBy(MovieCategories.movie)
						.having { MovieCategories.movie.count() eq it.size.toLong() }
				}
			}.alias("q1")

			var columnActor: Column<*> = MovieTable.id
			val viaActor = when (actors) {
				null, "null" -> MovieTable.select(MovieTable.id)
				"0" -> MovieTable
					.join(MovieActors, JoinType.LEFT, MovieTable.id, MovieActors.movie)
					.select(MovieTable.id)
					.where { MovieActors.actor.isNull() }

				else -> actors.split(",").map { it.toInt() }.let {
					columnActor = MovieActors.movie
					MovieActors
						.select(MovieActors.movie)
						.where { MovieActors.actor inList it }
						.groupBy(MovieActors.movie)
						.having { MovieActors.movie.count() eq it.size.toLong() }
				}
			}.alias("q2")

			mapOf(
				"movies" to Join(MovieTable)
					.join(viaActor, JoinType.LEFT, onColumn = MovieTable.id, otherColumn = viaActor[columnActor])
					.join(viaCategory, JoinType.LEFT, onColumn = MovieTable.id, otherColumn = viaCategory[columnCategory])
					.selectAll().where { viaActor[columnActor].isNotNull() and viaCategory[columnCategory].isNotNull() }
					.groupBy(MovieTable.id)
					.apply {
						when (call.request.queryParameters["filter"]?.toIntOrNull()) {
							1 -> orderBy(MovieTable.duration, SortOrder.DESC)
							else -> orderBy(MovieTable.id, SortOrder.DESC)
						}
					}
					.limit(call.parameters["count"]!!.toInt())
					.offset(call.parameters["from"]!!.toLong())
					.map { MoviePojo(it[MovieTable.id].value, it[MovieTable.name], it[MovieTable.duration]) },
				"count" to Join(MovieTable)
					.join(viaActor, JoinType.LEFT, MovieTable.id, viaActor[columnActor])
					.join(viaCategory, JoinType.LEFT, MovieTable.id, viaCategory[columnCategory])
					.selectAll().where { viaActor[columnActor].isNotNull() and viaCategory[columnCategory].isNotNull() }
					.groupBy(MovieTable.id)
					.orderBy(MovieTable.id, SortOrder.ASC)
					.count()
			)
		})
	}
	get("/movie/{id}") {
		call.respond(transaction {
			mapOf<String, Any>(
				"categories" to MovieCategories.selectAll().where { MovieCategories.movie eq call.parameters["id"]!!.toInt() }
					.map { it[MovieCategories.category].toString() },
				"actors" to MovieActors.selectAll().where { MovieActors.movie eq call.parameters["id"]!!.toInt() }
					.map { it[MovieActors.actor].toString() }
			)
		})
	}
	delete("/movie/{id}") {
		call.parameters["id"]?.toIntOrNull()?.also { id ->
			val path = transaction {
				val ret = MovieTable.selectAll().where { MovieTable.id eq id }.first()[MovieTable.path]
				MovieTable.deleteWhere { MovieTable.id eq id }
				MovieActors.deleteWhere { MovieActors.movie eq id }
				MovieCategories.deleteWhere { MovieCategories.movie eq id }
				DetectedDuplicatesTable.deleteWhere { DetectedDuplicatesTable.movie eq id }
				DetectedDuplicatesTable.deleteWhere { DetectedDuplicatesTable.otherMovie eq id }
				MoviesToBeCheckedTable.deleteWhere { MoviesToBeCheckedTable.id eq id }
				AudioToBeCheckedTable.deleteWhere { AudioToBeCheckedTable.id eq id }
				ret
			}
			File(path).delete()
			call.respond("{}")
		}
	}
}