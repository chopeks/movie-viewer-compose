package pl.chopeks.movies.server.services

import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.kodein.di.DI
import org.kodein.di.instance
import pl.chopeks.core.IVideoPlayer
import pl.chopeks.core.data.repository.IVideoRepository
import pl.chopeks.core.model.Actor
import pl.chopeks.core.model.Category
import pl.chopeks.core.model.Video

fun Route.movieService(di: DI) {
	val repository by di.instance<IVideoRepository>()
	val videoPlayer by di.instance<IVideoPlayer>()

	get("/movie/play/{id}") {
		videoPlayer.play(Video(call.parameters["id"]!!.toInt(), "", null))
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