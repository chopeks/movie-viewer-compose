package pl.chopeks.core.database.model


data class DuplicatePojo(
  val id: Int,
  val name: String,
  val duration: Int,
  val size: String
)

data class DuplicatesPojo(
  val list: List<DuplicatePojo>
)