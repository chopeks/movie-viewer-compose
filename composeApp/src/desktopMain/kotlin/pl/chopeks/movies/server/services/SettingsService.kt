package pl.chopeks.movies.server.services

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.kodein.di.DI
import org.kodein.di.instance
import pl.chopeks.core.data.repository.ISettingsRepository
import pl.chopeks.core.model.Settings

fun Route.settingsService(di: DI) {
  val repository by di.instance<ISettingsRepository>()

  get("/settings") {
    call.respond(HttpStatusCode.OK, repository.getSettings())
  }
  post("/settings") {
    runCatching { call.receiveNullable<Settings>() }.getOrNull()?.let {
      repository.setSettings(it)
    }
    call.respond(HttpStatusCode.OK, repository.getSettings())
  }
}