package pl.chopeks.movies.server.model

import kotlinx.serialization.Serializable

@Serializable
data class SettingsPojo(
  val browser: String,
  val moviePlayer: String
)