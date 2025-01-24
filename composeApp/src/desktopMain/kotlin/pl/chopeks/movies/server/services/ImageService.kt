package pl.chopeks.movies.server.services

import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import pl.chopeks.movies.server.db.MovieTable
import pl.chopeks.movies.server.db.MovieTable.thumbnail
import pl.chopeks.movies.server.model.Actor
import pl.chopeks.movies.server.model.Category
import pl.chopeks.movies.server.utils.makeScreenshot
import pl.chopeks.movies.server.utils.normalizeImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.*
import javax.imageio.ImageIO

fun Route.imageService() {
  get("/image/category/{id}") {
    call.respond(transaction { arrayOf(Category.findById(call.parameters["id"]!!.toInt())?.image) })
  }
  get("/image/actor/{id}") {
    call.respond(transaction { arrayOf(Actor.findById(call.parameters["id"]!!.toInt())?.image) })
  }
  get("/image/movie/{id}") {
    val image = transaction {
      MovieTable.selectAll().where { MovieTable.id eq call.parameters["id"]!!.toInt() }.firstOrNull().also {
        if (it != null) {
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
    }
    call.respond(arrayOf(image))
  }
}