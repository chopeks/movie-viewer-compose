package pl.chopeks.movies.screen

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import db.*
import db.PathsTable.path
import db.SchemaVerionsTable.version
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import pl.chopeks.movies.findDatabase
import utils.RefreshUtils
import utils.getFiles
import java.io.File
import java.sql.Connection

class PreloadScreenModel: ScreenModel {

  var isDone by mutableStateOf(false)

  fun init() {
    Database.connect("jdbc:sqlite:${findDatabase().absolutePath}", driver = "org.sqlite.JDBC")
    TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_READ_UNCOMMITTED
    transaction {
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

          else -> break@loop
        }
      }
    }
    // delete files that were removed
    transaction {
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

    RefreshUtils.refresh()
    isDone = true
  }
}