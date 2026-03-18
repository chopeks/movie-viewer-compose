package pl.chopeks.core.database

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import pl.chopeks.core.database.PathsTable.path
import pl.chopeks.core.database.SchemaVerionsTable.version
import pl.chopeks.core.utils.getFiles
import java.io.File
import java.sql.Connection

object DatabaseHelper {
	internal val defaultUrl: String
		get() = "jdbc:sqlite:${findDatabase().absolutePath}"
	internal const val defaultDriver: String = "org.sqlite.JDBC"

	fun clean(db: Database) {
		transaction(db) {
			MovieTable.selectAll().forEach {
				val movieId = it[MovieTable.id]
				if (!File(it[MovieTable.path]).exists()) {
					MovieTable.deleteWhere { MovieTable.id eq movieId }
				}
			}
			PathsTable.selectAll().forEach { table ->
				PathsTable.update({ path eq table[path] }) {
					it[count] = getFiles(File(table[path])).size
				}
			}
		}
	}

	internal fun connect(url: String = defaultUrl, driver: String = defaultDriver): Database {
		val db = Database.connect(url, driver = driver)
		TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_READ_UNCOMMITTED

		transaction(db) {
			SchemaUtils.create(SchemaVerionsTable)
			if (SchemaVerionsTable.selectAll().count() == 0L) {
				SchemaVerionsTable.insert {
					it[version] = 1
				}
			}
			loop@ while (true) {
				when (SchemaVerionsTable.selectAll().first()[version]) {
					1 -> {
						SchemaUtils.create(CategoryTable, ActorTable, MovieTable, MovieCategories, MovieActors, PathsTable)
						SchemaVerionsTable.inc()
					}

					2 -> { // add file count for each path
						SchemaUtils.createMissingTablesAndColumns(PathsTable)
						SchemaVerionsTable.inc()
					}

					3 -> { // add file count for each path
						SchemaUtils.createMissingTablesAndColumns(MoviesToBeCheckedTable, DetectedDuplicatesTable)
						SchemaVerionsTable.inc()
					}

					4 -> { // add file count for each path
						SchemaUtils.createMissingTablesAndColumns(AudioToBeCheckedTable)
						SchemaVerionsTable.inc()
					}

					5 -> {
						SchemaUtils.createMissingTablesAndColumns(DetectedDuplicatesTable)
						SchemaVerionsTable.inc()
					}

					else -> break@loop
				}
			}
		}

		transaction(db) {
			SchemaUtils.addMissingColumnsStatements(MovieTable).forEach(::exec)
			SchemaUtils.addMissingColumnsStatements(AudioToBeCheckedTable).forEach(::exec)
		}

		transaction(db) { // idk why isn't it working with exposed apis, but this works somehow
			exec("CREATE INDEX IF NOT EXISTS idx_movie_categories_movie ON movie_category (movie)")
			exec("CREATE INDEX IF NOT EXISTS idx_movie_categories_cat ON movie_category (category)")
			exec("CREATE INDEX IF NOT EXISTS idx_movie_actors_movie ON movie_actor (movie)")
			exec("CREATE INDEX IF NOT EXISTS idx_movie_actors_actor ON movie_actor (actor)")
		}

		return db
	}

	internal fun findDatabase(): File {
		val dir = File(System.getProperty("user.dir"))
		if (File(dir, "movies.sqlite").exists())
			return File(dir, "movies.sqlite")
		return File(File(dir, ".."), "movies.sqlite")
	}
}