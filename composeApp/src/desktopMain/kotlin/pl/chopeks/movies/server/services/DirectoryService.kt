package pl.chopeks.movies.server.services

import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.kodein.di.DI
import org.kodein.di.instance
import pl.chopeks.core.data.repository.ISettingsRepository
import pl.chopeks.core.model.Path
import pl.chopeks.movies.server.utils.RefreshUtils
import kotlin.concurrent.thread

fun Route.directoryService(di: DI) {
	val repository by di.instance<ISettingsRepository>()

	get("/directories") {
		call.respond(repository.getPaths())
	}
	post("/directory") {
		runCatching { call.receiveNullable<Path>() }.getOrNull()?.let { json ->
			repository.addPath(json.path)
			call.respond("{}")
		}
		thread { RefreshUtils.refresh {} }
	}
	post("/directory/remove") {
		runCatching { call.receiveNullable<Path>() }.getOrNull()?.let { json ->
			repository.removePath(json)
		}
		call.respond("{}")
	}
}