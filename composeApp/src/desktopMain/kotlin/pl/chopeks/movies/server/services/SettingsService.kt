package pl.chopeks.movies.server.services

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import pl.chopeks.core.database.cache.Cache
import pl.chopeks.core.model.Settings

fun Route.settingsService() {
  get("/settings") {
    call.respond(HttpStatusCode.OK, Cache.settings)
  }
  post("/settings") {
    kotlin.runCatching { call.receiveNullable<Settings>() }.getOrNull()?.let {
      Cache.settings = it
    }
    call.respond(HttpStatusCode.OK, Cache.settings)
  }
}