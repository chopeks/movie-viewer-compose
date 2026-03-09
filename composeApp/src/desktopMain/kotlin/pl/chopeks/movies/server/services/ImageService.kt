package pl.chopeks.movies.server.services

import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.kodein.di.DI
import org.kodein.di.instance
import pl.chopeks.core.IImageConverter
import pl.chopeks.core.data.repository.IActorRepository
import pl.chopeks.core.data.repository.ICategoryRepository
import pl.chopeks.core.data.repository.IVideoRepository
import pl.chopeks.core.database.MovieTable
import pl.chopeks.core.database.MovieTable.thumbnail
import pl.chopeks.core.model.Actor
import pl.chopeks.core.model.Category
import pl.chopeks.core.model.Video
import pl.chopeks.movies.server.utils.normalizeImage
import java.io.ByteArrayOutputStream
import java.util.*
import javax.imageio.ImageIO

fun Route.imageService(di: DI) {
	val actorRepository by di.instance<IActorRepository>()
	val categoryRepository by di.instance<ICategoryRepository>()
	val videoRepository by di.instance<IVideoRepository>()

	get("/image/category/{id}") {
		call.respond(arrayOf(categoryRepository.getImage(Category(call.parameters["id"]!!.toInt()))))
	}

	get("/image/actor/{id}") {
		call.respond(arrayOf(actorRepository.getImage(Actor(call.parameters["id"]!!.toInt()))))
	}

	get("/image/movie/{id}") {
		if (call.request.queryParameters["refresh"] == "true") {
			call.respond(arrayOf("data:image/jpg;base64," + videoRepository.refreshImage(Video(call.parameters["id"]!!.toInt(), "", null))))
		} else {
			call.respond(arrayOf("data:image/jpg;base64," + videoRepository.getImage(Video(call.parameters["id"]!!.toInt(), "", null))))
		}
	}
}