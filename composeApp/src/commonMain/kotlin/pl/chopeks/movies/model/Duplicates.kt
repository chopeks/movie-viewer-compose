package pl.chopeks.movies.model

import kotlinx.serialization.Serializable

@Serializable
data class Duplicates(
  val list: List<Video>
)

@Serializable
data class DuplicatesCount(
  val count: Int
)