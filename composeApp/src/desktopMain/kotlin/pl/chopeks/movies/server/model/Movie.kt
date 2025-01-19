package pl.chopeks.movies.server.model

data class MoviePojo(
  val id: Int,
  val name: String,
  val duration: Int?,
  val size: String? = ""
)

