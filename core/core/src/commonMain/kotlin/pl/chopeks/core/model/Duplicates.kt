package pl.chopeks.core.model

import kotlinx.serialization.Serializable
import pl.chopeks.core.model.Video

@Serializable
data class Duplicates(
  val list: List<Video>
)

@Serializable
data class DuplicatesCount(
  val count: Int
)