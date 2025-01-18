package pl.chopeks.movies.model

import kotlinx.serialization.Serializable

@Serializable
data class Actor(
  val id: Int,
  val name: String,
  var image: String? = null,
)