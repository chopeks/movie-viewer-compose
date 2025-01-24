package pl.chopeks.movies.model

import kotlinx.serialization.Serializable

@Serializable
data class Settings(
  var browser: String,
  var moviePlayer: String
)

@Serializable
data class Path(
  val path: String,
  val count: Int
)