package pl.chopeks.core.model

import kotlinx.serialization.Serializable

@Serializable
data class Category(
  val id: Int = 0,
  val name: String = "",
  var image: String? = null,
  var imageBytes: ByteArray? = null,
)