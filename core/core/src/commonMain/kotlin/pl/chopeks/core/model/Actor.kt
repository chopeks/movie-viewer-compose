package pl.chopeks.core.model

import kotlinx.serialization.Serializable

@Serializable
data class Actor(
  val id: Int,
  val name: String,
  var image: String? = null,
  val imageBytes: ByteArray? = null
)