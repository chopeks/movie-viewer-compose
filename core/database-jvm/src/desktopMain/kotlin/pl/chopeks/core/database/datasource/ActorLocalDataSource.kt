package pl.chopeks.core.database.datasource

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import pl.chopeks.core.database.ActorTable
import pl.chopeks.core.database.AudioToBeCheckedTable
import pl.chopeks.core.database.MovieActors
import pl.chopeks.core.database.model.ActorEntity
import pl.chopeks.core.model.Actor
import pl.chopeks.core.model.Video

class ActorLocalDataSource(
	private val db: Database
) {
	suspend fun getActors(): List<Actor> = withContext(Dispatchers.IO) {
		transaction(db) { ActorEntity.all().sortedBy { it.name.lowercase() }.map { it.pojo } }
	}

	suspend fun getImage(actor: Actor): String? = withContext(Dispatchers.IO) {
		transaction(db) {
			ActorEntity.findById(actor.id)?.image?.substringAfter(",")
		}
	}

	suspend fun bind(actor: Actor, video: Video) {
		withContext(Dispatchers.IO) {
			transaction(db) {
				MovieActors.upsert {
					it[MovieActors.movie] = video.id
					it[MovieActors.actor] = actor.id
				}
				AudioToBeCheckedTable.insert {
					it[AudioToBeCheckedTable.videoId] = video.id
				}
			}
		}
	}

	suspend fun unbind(actor: Actor, video: Video) = withContext(Dispatchers.IO) {
		transaction(db) {
			MovieActors.deleteWhere { (MovieActors.movie eq video.id and (MovieActors.actor eq actor.id)) }
		}
	}

	suspend fun edit(id: Int, name: String, url: String?) {
		withContext(Dispatchers.IO) {
			transaction(db) {
				if (ActorTable.selectAll().where { ActorTable.id eq id }.firstOrNull() != null) {
					ActorTable.update({ ActorTable.id eq id }) { obj ->
						obj[ActorTable.name] = name
						obj[ActorTable.image] = url?.ifBlank { null }
					}
				} else {
					ActorTable.insert { new ->
						new[ActorTable.name] = name
						new[ActorTable.image] = url?.ifBlank { null }
					}
				}
			}
		}
	}

	suspend fun delete(actor: Actor) = withContext(Dispatchers.IO) {
		transaction(db) {
			ActorTable.deleteWhere { ActorTable.id eq actor.id }
			MovieActors.deleteWhere { MovieActors.actor eq actor.id }
		}
	}

	suspend fun findVideosWithSharedActors(videoId: Int) = withContext(Dispatchers.IO) {
		transaction(db) {
			val actors = MovieActors.selectAll()
				.where { MovieActors.movie eq videoId }
				.map { it[MovieActors.actor] }
			MovieActors.selectAll()
				.where { MovieActors.actor inList actors }
				.withDistinct(true)
				.map { it[MovieActors.movie] }
		}
	}
}