package pl.chopeks.movies.server.services

import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.kodein.di.DI
import org.kodein.di.instance
import pl.chopeks.core.data.repository.IDuplicateRepository
import pl.chopeks.core.model.Actor
import pl.chopeks.core.model.Duplicates
import pl.chopeks.core.model.DuplicatesCount
import pl.chopeks.core.model.Video

fun Route.duplicatesService(di: DI) {
	val repository by di.instance<IDuplicateRepository>()

//	get("/duplicates") { // todo, what was that for?...
//		call.respond(transaction {
//			val joinColumn: Column<*> = MovieTable.duration
//			val inner = MovieTable.select(MovieTable.duration, MovieTable.duration.count())
//				.groupBy(MovieTable.duration)
//				.having { MovieTable.id.count() greater 1 }
//				.alias("q1")
//
//			MovieTable.join(inner, JoinType.INNER, onColumn = MovieTable.duration, otherColumn = inner[joinColumn])
//				.selectAll()
//				.orderBy(MovieTable.duration)
//				.groupBy({
//					it[MovieTable.duration]
//				}, {
//					MoviePojo(it[MovieTable.id].value, it[MovieTable.name], it[MovieTable.duration], "%.2f MB".format(File(it[MovieTable.path]).length() / 1024.0 / 1024.0))
//				})
//				.toList()
//				.map {
//					mapOf(
//						"duration" to it.first,
//						"movie" to it.second
//					)
//				}
//		})
//	}

	get("/certain_duplicates") {
		call.respond<List<Duplicates>>(repository.getCertainDuplicates())
	}

	get("/duplicates/left") {
		call.respond<DuplicatesCount>(DuplicatesCount(repository.count()))
	}

	delete("/duplicates/cancel/{id}/{otherId}") {
		val id = call.parameters["id"]?.toIntOrNull()!!
		val otherId = call.parameters["otherId"]?.toIntOrNull()!!
		repository.cancel(Duplicates(listOf(Video(id, "", null), Video(otherId, "", null))))
		call.respond("{}")
	}

	get("/duplicates/dedup/actor/{actor}") {
		val actor = call.parameters["actor"]?.toIntOrNull()!!
		repository.deduplicate(Actor(actor))
		call.respond("{}")
	}

	get("/duplicates/dedup/all") {
		repository.deduplicateAll()
		call.respond("{}")
	}
}