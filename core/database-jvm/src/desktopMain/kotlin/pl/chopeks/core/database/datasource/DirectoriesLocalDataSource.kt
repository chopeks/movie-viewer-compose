package pl.chopeks.core.database.datasource

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import pl.chopeks.core.database.MovieTable
import pl.chopeks.core.database.PathsTable
import pl.chopeks.core.database.PathsTable.path
import pl.chopeks.core.model.Path
import pl.chopeks.core.utils.getFiles
import java.io.File

class DirectoriesLocalDataSource(
	private val db: Database
) {
	fun getPaths(): List<Path> {
		return transaction(db) {
			PathsTable.selectAll().map { Path(it[path], it[PathsTable.count]) }
		}
	}

	fun remove(path: Path) {
		transaction(db) {
			PathsTable.deleteWhere { PathsTable.path eq path.path }
			MovieTable.deleteWhere { MovieTable.path like "${path}%" }
		}
	}

	fun add(path: String) {
		transaction(db) {
			try {
				PathsTable.insert {
					it[PathsTable.path] = path
					it[count] = getFiles(File(path)).size
				}
			} catch (e: Throwable) {
			}
		}
	}
}