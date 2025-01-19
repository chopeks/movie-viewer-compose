package pl.chopeks.movies.server.model

import pl.chopeks.movies.server.db.CategoryTable
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class Category(id: EntityID<Int>) : IntEntity(id) {
  companion object : IntEntityClass<Category>(CategoryTable)

  var name by CategoryTable.name
  var image by CategoryTable.image

  val pojo
    get() = CategoryPojo(id.value, name, null)
}

data class CategoryPojo(
  val id: Int,
  val name: String,
  var image: String?
)