package pl.chopeks.movies.server.services

import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.kodein.di.DI
import org.kodein.di.instance
import pl.chopeks.core.data.repository.IVideoRepository
import pl.chopeks.core.database.*
import pl.chopeks.core.database.cache.Cache
import pl.chopeks.core.model.Actor
import pl.chopeks.core.model.Category
import pl.chopeks.core.model.Video
import pl.chopeks.movies.server.utils.runCommand
import java.io.File

fun Route.movieService(di: DI) {
	val repository by di.instance<IVideoRepository>()

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
		val categories = call.request.queryParameters["category"]?.split(",")?.map { it.toIntOrNull() }?.filterNotNull()
		val actors = call.request.queryParameters["actor"]?.split(",")?.map { it.toIntOrNull() }?.filterNotNull()
		val filter = call.request.queryParameters["filter"]?.toIntOrNull() ?: 0
		val count = call.request.queryParameters["count"]?.toIntOrNull() ?: 15
		val from = call.request.queryParameters["from"]?.toLongOrNull() ?: 0L

		call.respond(
			HttpStatusCode.OK, repository.getVideos(
				from,
				actors = when {
					actors == null -> emptyList()
					actors.contains(0) -> listOf(Actor(0, ""))
					else -> actors.map { Actor(it, "") }
				},
				categories = when {
					categories == null -> emptyList()
					categories.contains(0) -> listOf(Category(0, ""))
					else -> categories.map { Category(it, "") }
				},
				filter, count)
		)
	}
	get("/movie/{id}") {
		call.parameters["id"]?.toInt()!!.also { id ->
			call.respond(HttpStatusCode.OK, repository.getInfo(Video(id, "", null)))
		}
	}
	delete("/movie/{id}") {
		call.parameters["id"]?.toInt()!!.also { id ->
			repository.remove(Video(id, "", null))
			call.respond("{}")
		}
	}
}