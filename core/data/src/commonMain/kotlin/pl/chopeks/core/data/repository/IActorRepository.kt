package pl.chopeks.core.data.repository

import kotlinx.rpc.annotations.Rpc
import pl.chopeks.core.model.Actor
import pl.chopeks.core.model.Video

@Rpc
interface IActorRepository {
	suspend fun getActors(): List<Actor>
	suspend fun getImage(actor: Actor): String?
	suspend fun getImageBytes(actor: Actor): ByteArray?
	suspend fun bind(actor: Actor, video: Video)
	suspend fun unbind(actor: Actor, video: Video)
	suspend fun add(name: String, url: String?)
	suspend fun edit(id: Int, name: String, url: String?)
	suspend fun delete(actor: Actor)
}