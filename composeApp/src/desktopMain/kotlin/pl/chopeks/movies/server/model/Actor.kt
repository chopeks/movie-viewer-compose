package pl.chopeks.movies.server.model

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import pl.chopeks.core.database.ActorTable

class Actor(id: EntityID<Int>) : IntEntity(id) {
	companion object : IntEntityClass<Actor>(ActorTable)

	var name by ActorTable.name
	var image by ActorTable.image

	val pojo
		get() = ActorPojo(id.value, name, null)
}

data class ActorPojo(
	val id: Int,
	val name: String,
	var image: String?
)