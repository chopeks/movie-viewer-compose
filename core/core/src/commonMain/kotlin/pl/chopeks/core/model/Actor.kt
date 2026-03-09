package pl.chopeks.core.model

import kotlinx.serialization.Serializable

@Serializable
data class Actor(
  val id: Int = 0,
  val name: String = "",
  var image: String? = null,
  val imageBytes: ByteArray? = null
)