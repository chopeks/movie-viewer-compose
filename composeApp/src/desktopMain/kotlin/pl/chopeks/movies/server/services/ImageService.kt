package pl.chopeks.movies.server.services

import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.kodein.di.DI
import org.kodein.di.instance
import pl.chopeks.core.data.repository.IActorRepository
import pl.chopeks.core.data.repository.ICategoryRepository
import pl.chopeks.core.database.MovieTable
import pl.chopeks.core.database.MovieTable.thumbnail
import pl.chopeks.core.model.Actor
import pl.chopeks.core.model.Category
import pl.chopeks.movies.server.utils.makeScreenshot
import pl.chopeks.movies.server.utils.normalizeImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.*
import javax.imageio.ImageIO

fun Route.imageService(di: DI) {
	val actorRepository by di.instance<IActorRepository>()
	val categoryRepository by di.instance<ICategoryRepository>()

	get("/image/category/{id}") {
		call.respond(arrayOf(categoryRepository.getImage(Category(call.parameters["id"]!!.toInt()))))
	}
	
	get("/image/actor/{id}") {
		call.respond(arrayOf(actorRepository.getImage(Actor(call.parameters["id"]!!.toInt()))))
	}

	get("/image/movie/{id}") {
		val image = transaction {
			MovieTable.selectAll().where { MovieTable.id eq call.parameters["id"]!!.toInt() }.firstOrNull().also {
				if (it == null)
					return@also

				if (it[thumbnail] != null && call.request.queryParameters["refresh"] != "true") {
					return@transaction it[thumbnail]
				} else {
					val images = mutableListOf<String>()
					makeScreenshot(File(it[MovieTable.path]), (1..999).random().toLong()).also { img ->
						val bytes = ImageIO.read(img.inputStream())
							.normalizeImage()
							.let {
								ByteArrayOutputStream().use { os ->
									ImageIO.write(it, "jpg", os)
									os.toByteArray()
								}
							}
						transaction {
							MovieTable.update({ MovieTable.id eq call.parameters["id"]!!.toInt() }, body = {
								it[thumbnail] = "data:image/jpg;base64," + String(Base64.getMimeEncoder().encode(bytes))
							})
						}
						images.add("data:image/jpg;base64," + String(Base64.getMimeEncoder().encode(bytes)))
					}
					return@transaction images[0]
				}
			}
		}
		call.respond(arrayOf(image))
	}
}