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
		get() = "jdbc:sqlite:${findDatabase().absolutePath}?foreign_keys=on"
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
			addLogger(StdOutSqlLogger)

			val currentVersion = 6 // Update this manually as you add versions

			SchemaUtils.create(SchemaVerionsTable)
			if (SchemaVerionsTable.selectAll().count() == 0L) {
				SchemaVerionsTable.insert {
					it[version] = 1
				}
			}
			loop@ while (true) {
				when (SchemaVerionsTable.selectAll().first()[version]) {
					1 -> {
						SchemaUtils.create(
							CategoryTable, ActorTable, MovieTable,
							MovieCategories, MovieActors, PathsTable,
							MoviesToBeCheckedTable, AudioToBeCheckedTable,
							DetectedDuplicatesTable, SchemaVerionsTable
						)
						SchemaVerionsTable.set(currentVersion)
						return@transaction
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

					6 -> {
						SchemaUtils.createMissingTablesAndColumns(MoviesToBeCheckedTable)
						exec("DELETE FROM movie_category WHERE movie NOT IN (SELECT id FROM movie)")
						exec("DELETE FROM movie_actor WHERE movie NOT IN (SELECT id FROM movie)")
						exec("DELETE FROM tbc WHERE vid NOT IN (SELECT id FROM movie)")
						exec("DELETE FROM atbc WHERE vid NOT IN (SELECT id FROM movie)")
						exec("DELETE FROM dup WHERE movie NOT IN (SELECT id FROM movie) OR other NOT IN (SELECT id FROM movie)")
						SchemaVerionsTable.inc()
					}

					else -> break@loop
				}
			}
		}

		transaction(db) {
			addLogger(StdOutSqlLogger)
			SchemaUtils.addMissingColumnsStatements(MovieTable, MovieActors, MovieCategories).forEach(::exec)
			SchemaUtils.addMissingColumnsStatements(AudioToBeCheckedTable).forEach(::exec)
		}

		transaction(db) {
			exec("CREATE INDEX IF NOT EXISTS idx_movie_categories_movie ON movie_category (movie)")
			exec("CREATE INDEX IF NOT EXISTS idx_movie_categories_cat ON movie_category (category)")
			exec("CREATE INDEX IF NOT EXISTS idx_movie_actors_movie ON movie_actor (movie)")
			exec("CREATE INDEX IF NOT EXISTS idx_movie_actors_actor ON movie_actor (actor)")

			val triggers = listOf(
				"movie_category" to "movie",
				"movie_actor" to "movie",
				"tbc" to "vid",
				"atbc" to "vid"
			)

			triggers.forEach { (table, column) ->
				exec(
					"""
            CREATE TRIGGER IF NOT EXISTS t_clean_${table}_on_movie_del
            AFTER DELETE ON movie
            BEGIN
                DELETE FROM $table WHERE $column = OLD.id;
            END;
        """
				)
			}

			exec(
				"""
        CREATE TRIGGER IF NOT EXISTS t_clean_dup_on_movie_del
        AFTER DELETE ON movie
        BEGIN
            DELETE FROM dup WHERE movie = OLD.id OR other = OLD.id;
        END;
    """
			)
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