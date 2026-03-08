package pl.chopeks.core.data.repository

import pl.chopeks.core.database.datasource.ActorLocalDataSource
import pl.chopeks.core.model.Actor
import pl.chopeks.core.model.Video

class ActorRepository(
	private val dataSource: ActorLocalDataSource
): IActorRepository {
	override suspend fun getActors(): List<Actor> {
		return dataSource.getActors()
	}

	override suspend fun getImage(actor: Actor): String? {
		return dataSource.getImage(actor)
	}

	override suspend fun bind(actor: Actor, video: Video) {
		dataSource.bind(actor, video)
	}

	override suspend fun unbind(actor: Actor, video: Video) {
		dataSource.unbind(actor, video)
	}

	override suspend fun add(name: String, url: String) {
		dataSource.add(name, url)
	}

	override suspend fun edit(id: Int, name: String, url: String) {
		dataSource.edit(id, name, url)
	}

	override fun close() {
	}
}