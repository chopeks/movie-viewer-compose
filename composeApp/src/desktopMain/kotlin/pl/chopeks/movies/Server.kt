package pl.chopeks.movies

import io.ktor.http.*
import io.ktor.serialization.gson.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.*
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.kodein.di.DI
import pl.chopeks.movies.server.services.*
import java.text.DateFormat
import kotlin.time.Duration.Companion.days

fun Application.module(di: DI) {
	install(CORS) {
		allowMethod(HttpMethod.Options)
		allowMethod(HttpMethod.Get)
		allowMethod(HttpMethod.Post)
		allowMethod(HttpMethod.Delete)
		allowMethod(HttpMethod.Put)
		allowMethod(HttpMethod.Head)
		allowMethod(HttpMethod.Patch)
		anyHost()
		maxAgeDuration = 365.days
	}
	install(ContentNegotiation) {
		gson {
			setDateFormat(DateFormat.LONG)
			setPrettyPrinting()
		}
	}

	routing {
		static("/") {
			resource("index.html")
			resource("manifest.json")
			resource("service-worker.js")
			default("index.html")
		}
		get("/") {
			call.respondRedirect("/index.html", true)
		}

		actorService(di)
		categoryService(di)
		imageService(di)
		directoryService(di)
		movieService(di)
		duplicatesService(di)
		settingsService(di)
	}
}
