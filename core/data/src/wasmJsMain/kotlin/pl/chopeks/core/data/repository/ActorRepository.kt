package pl.chopeks.core.data.repository

import pl.chopeks.core.data.Backend
import pl.chopeks.core.data.utils.RpcWrapper
import pl.chopeks.core.model.Actor
import pl.chopeks.core.model.Video

class ActorRepository(
	private val delegate: IActorRepository
) : IActorRepository, RpcWrapper {
	override suspend fun getActors(): List<Actor> = rpc {
		delegate.getActors().map {
			it.copy(image = "${Backend.URL}api/image/${it.id}/actor")
		}
	}

	override suspend fun getImage(actor: Actor): String? = null
	override suspend fun getImageBytes(actor: Actor): ByteArray? = null

	override suspend fun bind(actor: Actor, video: Video) = rpc {
		delegate.bind(actor, video)
	}

	override suspend fun unbind(actor: Actor, video: Video) = rpc {
		delegate.unbind(actor, video)
	}

	override suspend fun add(name: String, url: String?) = rpc {
		delegate.add(name, url)
	}

	override suspend fun edit(id: Int, name: String, url: String?) = rpc {
		delegate.edit(id, name, url)
	}

	override suspend fun delete(actor: Actor) = rpc {
		delegate.delete(actor)
	}
}