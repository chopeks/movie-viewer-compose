package pl.chopeks.core.data.repository

import pl.chopeks.core.database.datasource.ActorLocalDataSource
import pl.chopeks.core.model.Actor
import pl.chopeks.core.model.Video
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class ActorRepository(
	private val dataSource: ActorLocalDataSource
) : IActorRepository {
	override suspend fun getActors(): List<Actor> {
		return dataSource.getActors()
	}

	override suspend fun getImage(actor: Actor): String? {
		return dataSource.getImage(actor)
	}

	@OptIn(ExperimentalEncodingApi::class)
	override suspend fun getImageBytes(actor: Actor): ByteArray? {
		return getImage(actor)?.let { Base64.Mime.decode(it) }
	}

	override suspend fun bind(actor: Actor, video: Video) {
		dataSource.bind(actor, video)
	}

	override suspend fun unbind(actor: Actor, video: Video) {
		dataSource.unbind(actor, video)
	}

	override suspend fun add(name: String, image: String?) {
		dataSource.edit(0, name, image)
	}

	override suspend fun edit(id: Int, name: String, image: String?) {
		dataSource.edit(id, name, image)
	}

	override suspend fun delete(actor: Actor) {
		dataSource.delete(actor)
	}
}
