package pl.chopeks.core.database.model

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import pl.chopeks.core.database.ActorTable
import pl.chopeks.core.model.Actor

internal class ActorEntity(id: EntityID<Int>) : IntEntity(id) {
	companion object : IntEntityClass<ActorEntity>(ActorTable)

	var name by ActorTable.name
	var image by ActorTable.image

	val pojo
		get() = Actor(id.value, name, null)
}
