package pl.chopeks.core.database

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update

object CategoryTable : IntIdTable("category") {
  val name = text("name")
  val image = text("image").nullable()
}

object ActorTable : IntIdTable("actor") {
  val name = text("name")
  val image = text("image").nullable()
}

object MovieTable : IntIdTable("movie") {
  val name = text("name")
  val path = text("path")
  val thumbnail = text("thumbnail").nullable()
  val duration = integer("duration").nullable()
  val fingerprint = binary("fingerprint").nullable()
  val needle = binary("needle").nullable()
}

object MovieCategories : IntIdTable("movie_category") {
  val movie = integer("movie").index("idx_movie_categories_movie")
  val category = integer("category").index("idx_movie_categories_cat")
}

object MovieActors : IntIdTable("movie_actor") {
  val movie = integer("movie").index("idx_movie_actors_movie")
  val actor = integer("actor").index("idx_movie_actors_actor")
}

object PathsTable : Table("paths") {
  val path = text("path")
  val count = integer("files").default(0)
}

object MoviesToBeCheckedTable : IntIdTable("tbc")

object AudioToBeCheckedTable : IntIdTable("atbc")

object DetectedDuplicatesTable: Table("dup") {
  val movie = integer("movie")
  val otherMovie = integer("other")
  val timestamp = integer("t").default(0)
  val otherTimestamp = integer("ot").default(0)
}

object SchemaVerionsTable : Table("schemasVer") {
  val version = integer("version")

  fun inc() {
    update({ version.isNotNull() }) {
      it[version] = selectAll().first()[version] + 1
    }
  }
}