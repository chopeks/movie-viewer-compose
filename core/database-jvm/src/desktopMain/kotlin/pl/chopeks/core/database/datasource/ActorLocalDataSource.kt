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
import pl.chopeks.core.database.model.CategoryEntity
import pl.chopeks.core.model.Actor
import pl.chopeks.core.model.Video

class ActorLocalDataSource(
	private val db: Database
) {
	suspend fun getActors(): List<Actor> {
		return withContext(Dispatchers.IO) {
			transaction(db) { ActorEntity.all().sortedBy { it.name.lowercase() }.map { it.pojo } }
		}
	}

	suspend fun getImage(actor: Actor): String? {
		return withContext(Dispatchers.IO) {
			transaction(db) {
				ActorEntity.findById(actor.id)?.image?.substringAfter(",")
			}
		}
	}

	suspend fun bind(actor: Actor, video: Video) {
		withContext(Dispatchers.IO) {
			val row = transaction(db) {
				MovieActors.selectAll().where { (MovieActors.movie eq video.id) and (MovieActors.actor eq actor.id) }
					.firstOrNull()
			}
			if (row != null)
				return@withContext

			transaction(db) {
				MovieActors.insert {
					it[MovieActors.movie] = video.id
					it[MovieActors.actor] = actor.id
				}
				AudioToBeCheckedTable.upsert {
					it[AudioToBeCheckedTable.id] = video.id
				}
			}
		}
	}

	suspend fun unbind(actor: Actor, video: Video) {
		withContext(Dispatchers.IO) {
			transaction(db) {
				MovieActors.deleteWhere { (MovieActors.movie eq video.id and (MovieActors.actor eq actor.id)) }
			}
		}
	}

	suspend fun add(name: String, url: String) {
		transaction(db) {
			ActorTable.upsert(ActorTable.id, ActorTable.name, ActorTable.image) { obj ->
				obj[ActorTable.name] = name
				obj[ActorTable.image] = url
			}
		}
	}

	suspend fun edit(id: Int, name: String, url: String) {
		transaction(db) {
			if (ActorTable.selectAll().where { ActorTable.id eq id }.firstOrNull() != null) {
				ActorTable.update({ ActorTable.id eq id }) { obj ->
					obj[ActorTable.name] = name
					obj[ActorTable.image] = url
				}
			} else {
				ActorTable.insert { new ->
					new[ActorTable.name] = name
					new[ActorTable.image] = url
				}
			}
		}
	}
}