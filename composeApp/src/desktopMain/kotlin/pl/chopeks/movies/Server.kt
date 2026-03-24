package pl.chopeks.movies

import io.ktor.http.*
import io.ktor.serialization.gson.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.maxAgeDuration
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.rpc.krpc.ktor.server.Krpc
import kotlinx.rpc.krpc.ktor.server.rpc
import kotlinx.rpc.krpc.serialization.json.json
import org.kodein.di.DI
import org.kodein.di.direct
import org.kodein.di.instance
import pl.chopeks.core.data.IVideoPlayer
import pl.chopeks.core.data.repository.*
import pl.chopeks.core.model.Actor
import java.text.DateFormat
import kotlin.time.Duration.Companion.days

fun Application.module(di: DI) {
	install(Krpc)
	install(CORS) {
		anyHost()
		allowNonSimpleContentTypes = true
		allowMethod(HttpMethod.Options)
		allowMethod(HttpMethod.Get)
		allowMethod(HttpMethod.Post)
		allowMethod(HttpMethod.Delete)
		allowMethod(HttpMethod.Put)
		allowMethod(HttpMethod.Head)
		allowMethod(HttpMethod.Patch)
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

		rpc("/rpc") {
			rpcConfig {
				serialization {
					json()
				}
			}
			registerService<IActorRepository> { di.direct.instance<IActorRepository>() }
			registerService<ISettingsRepository> { di.direct.instance<ISettingsRepository>() }
			registerService<ICategoryRepository> { di.direct.instance<ICategoryRepository>() }
			registerService<IVideoRepository> { di.direct.instance<IVideoRepository>() }
			registerService<IVideoPlayer> { di.direct.instance<IVideoPlayer>() }
			registerService<IDuplicateRepository> { di.direct.instance<IDuplicateRepository>() }
		}

		get("/api/image/{id}/actor") { // for some reason, this works better with browsers rather than streaming bytes over rpc
			val id = call.parameters["id"]?.toIntOrNull() ?: return@get call.respond(HttpStatusCode.BadRequest)
			val bytes = di.direct.instance<IActorRepository>().getImageBytes(Actor(id))
			if (bytes != null) {
				call.respondBytes(bytes, contentType = ContentType.Image.Any)
			} else {
				call.respond(HttpStatusCode.NotFound)
			}
		}
	}
}
