package services

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import model.SettingsPojo
import utils.Cache

fun Route.settingsService() {
  get("/settings") {
    call.respond(HttpStatusCode.OK, Cache.settings)
  }
  post("/settings") {
    kotlin.runCatching { call.receiveNullable<SettingsPojo>() }.getOrNull()?.let {
      Cache.settings = it
    }
    call.respond(HttpStatusCode.OK, Cache.settings)
  }
}