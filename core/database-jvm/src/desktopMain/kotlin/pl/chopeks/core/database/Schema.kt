package pl.chopeks.core.database

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update

internal object CategoryTable : IntIdTable("category") {
	val name = text("name")
	val image = text("image").nullable()
}

internal object ActorTable : IntIdTable("actor") {
	val name = text("name")
	val image = text("image").nullable()
}

internal object MovieTable : IntIdTable("movie") {
	val name = text("name")
	val path = text("path")
	val thumbnail = text("thumbnail").nullable()
	val duration = integer("duration").nullable()
	val fingerprint = binary("fingerprint").nullable()
	val needle = binary("needle").nullable()
}

internal object MovieCategories : IntIdTable("movie_category") {
	val movie = integer("movie")
		.references(MovieTable.id)
		.index("idx_movie_categories_movie")
	val category = integer("category")
		.references(CategoryTable.id)
		.index("idx_movie_categories_cat")
}

internal object MovieActors : IntIdTable("movie_actor") {
	val movie = integer("movie")
		.references(MovieTable.id)
		.index("idx_movie_actors_movie")
	val actor = integer("actor")
		.references(ActorTable.id)
		.index("idx_movie_actors_actor")
}

internal object PathsTable : Table("paths") {
	val path = text("path")
	val count = integer("files").default(0)
}

internal object MoviesToBeCheckedTable : IntIdTable("tbc") {
	val videoId = integer("vid")
		.default(0)
		.references(MovieTable.id)
}

internal object AudioToBeCheckedTable : IntIdTable("atbc") {
	val videoId = integer("vid")
		.default(0)
		.references(MovieTable.id)
}

internal object DetectedDuplicatesTable : Table("dup") {
	val movie = integer("movie")
		.references(MovieTable.id)
	val otherMovie = integer("other")
		.references(MovieTable.id)
	val timestamp = integer("t")
		.default(0)
	val otherTimestamp = integer("ot")
		.default(0)
}

internal object SchemaVerionsTable : Table("schemasVer") {
	val version = integer("version")

	fun inc() {
		update({ version.isNotNull() }) {
			it[version] = selectAll().first()[version] + 1
		}
	}

	fun set(ver: Int) {
		update({ version.isNotNull() }) {
			it[version] = ver
		}
	}
}