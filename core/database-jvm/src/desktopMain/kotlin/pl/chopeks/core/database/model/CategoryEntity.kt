package pl.chopeks.core.database.model

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import pl.chopeks.core.database.CategoryTable
import pl.chopeks.core.model.Category

internal class CategoryEntity(id: EntityID<Int>) : IntEntity(id) {
  companion object : IntEntityClass<CategoryEntity>(CategoryTable)

  var name by CategoryTable.name
  var image by CategoryTable.image

  val pojo
    get() = Category(id.value, name, null)
}
