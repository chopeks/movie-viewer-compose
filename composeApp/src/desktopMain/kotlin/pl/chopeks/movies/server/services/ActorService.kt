package pl.chopeks.movies.server.services

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.kodein.di.DI
import org.kodein.di.instance
import pl.chopeks.core.data.repository.IActorRepository
import pl.chopeks.core.model.Actor
import pl.chopeks.core.model.Video
import pl.chopeks.movies.server.utils.urlImageToBase64

fun Route.actorService(di: DI) {
	val repository by di.instance<IActorRepository>()
	// get all actors
	get("/actors") {
		call.respond(repository.getActors())
	}
	//region crud
	get("/actor/{id}") {
		call.respond(repository.getActor(call.parameters["id"]!!.toInt()) ?: HttpStatusCode.NotFound)
	}

	post("/actor") {
		runCatching { call.receiveNullable<Actor>() }.getOrNull()?.let {
			if (it.image?.startsWith("http") == true) {
				it.image = it.image?.urlImageToBase64(269, 384)
			}
			if (it.id == 0) {
				repository.add(it.name, it.image ?: "")
			} else {
				repository.edit(it.id, it.name, it.image  ?: "")
			}
			call.respond(HttpStatusCode.OK, "{}")
		}
	}

	delete("/actor/{id}") {
		repository.delete(Actor(call.parameters["id"]!!.toInt(), ""))
		call.respond(HttpStatusCode.OK, "{}")
	}
	//endregion

	//region binding
	post("/actors/{actor}/{movie}") {
		repository.bind(Actor(call.parameters["actor"]!!.toInt(), ""), Video(call.parameters["movie"]!!.toInt(), "", null))
		call.respond("{}")
	}
	delete("/actors/{actor}/{movie}") {
		repository.unbind(Actor(call.parameters["actor"]!!.toInt(), ""), Video(call.parameters["movie"]!!.toInt(), "", null))
		call.respond("{}")
	}
	//endregion
}

