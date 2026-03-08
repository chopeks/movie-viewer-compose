package pl.chopeks.core.data.repository

import pl.chopeks.core.model.Actor
import pl.chopeks.core.model.Video

interface IActorRepository: AutoCloseable {
	suspend fun getActors(): List<Actor>
	suspend fun getImage(actor: Actor): String?
	suspend fun bind(actor: Actor, video: Video)
	suspend fun unbind(actor: Actor, video: Video)
	suspend fun add(name: String, url: String)
	suspend fun edit(id: Int, name: String, url: String)
}