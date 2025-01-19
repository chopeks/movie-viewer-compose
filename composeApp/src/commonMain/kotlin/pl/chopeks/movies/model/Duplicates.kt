package pl.chopeks.movies.model

import kotlinx.serialization.Serializable

@Serializable
data class Duplicates(
  val list: List<Video>
)
