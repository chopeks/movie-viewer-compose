package pl.chopeks.movies.screen

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import db.*
import db.PathsTable.path
import db.SchemaVerionsTable.version
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
  val events = mutableStateListOf<String>()

  fun init() {
    screenModelScope.launch(Dispatchers.IO) {
      Database.connect("jdbc:sqlite:${findDatabase().absolutePath}", driver = "org.sqlite.JDBC")
      events.add("Database connected.")
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

            3 -> { // add file count for each path
              SchemaUtils.createMissingTablesAndColumns(MoviesToBeCheckedTable, DetectedDuplicates)
              SchemaVerionsTable.inc()
            }

            else -> break@loop
          }
        }
        events.add("Schema updated.")
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
        events.add("Removed files purged.")
      }

      RefreshUtils.refresh(events::add)
      events.add("New files added.")
      isDone = true
    }
  }
}