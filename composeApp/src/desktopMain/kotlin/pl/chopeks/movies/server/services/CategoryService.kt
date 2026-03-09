package pl.chopeks.movies.server.services

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.kodein.di.DI
import org.kodein.di.instance
import pl.chopeks.core.data.repository.ICategoryRepository
import pl.chopeks.core.model.Category
import pl.chopeks.core.model.Video
import pl.chopeks.movies.server.utils.urlImageToBase64

fun Route.categoryService(di: DI) {
	val repository by di.instance<ICategoryRepository>()

	get("/categories") { call.respond(repository.getCategories()) }

	//region crud
	post("/category") {
		runCatching { call.receiveNullable<Category>() }.getOrNull()?.let {
			if (it.image?.startsWith("http") == true) {
				it.image = it.image?.urlImageToBase64(425, 240)
			}
			if (it.id == 0) {
				repository.add(it.name, it.image ?: "")
			} else {
				repository.edit(it.id, it.name, it.image ?: "")
			}
			call.respond(HttpStatusCode.OK, "{}")
		}
	}
	delete("/category/{id}") {
		repository.delete(Category(call.parameters["id"]!!.toInt(), ""))
		call.respond(HttpStatusCode.OK, "{}")
	}
	//endregion

	//region binding
	post("/categories/{category}/{movie}") {
		repository.bind(Category(call.parameters["actor"]!!.toInt(), ""), Video(call.parameters["movie"]!!.toInt(), "", null))
		call.respond("{}")
	}

	delete("/categories/{category}/{movie}") {
		repository.unbind(Category(call.parameters["id"]!!.toInt(), ""), Video(call.parameters["movie"]!!.toInt(), "", null))
		call.respond("{}")
	}
	//endregion
}