package pl.chopeks.core.model

import kotlinx.serialization.Serializable

@Serializable
data class Category(
  val id: Int,
  val name: String,
  var image: String? = null,
  var imageBytes: ByteArray? = null,
)