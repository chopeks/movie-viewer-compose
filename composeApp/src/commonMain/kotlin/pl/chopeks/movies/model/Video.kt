package pl.chopeks.movies.model

import kotlinx.serialization.Serializable

@Serializable
data class Video(
  val id: Int,
  val name: String,
  val duration: Long?,
  val size: String? = "",
  val image: String? = null,
  val chips: VideoChips? = null
)

@Serializable
data class VideoChips(
  val actors: List<Actor>,
  val categories: List<Category>
)

@Serializable
data class VideoInfo(
  val categories: List<Int>,
  val actors: List<Int>,
)

@Serializable
data class VideoContainer(
  val movies: List<Video>,
  val count: Long,
)

