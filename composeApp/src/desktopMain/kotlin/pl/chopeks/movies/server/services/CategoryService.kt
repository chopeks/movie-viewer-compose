package pl.chopeks.movies.server.services

import pl.chopeks.movies.server.db.CategoryTable
import pl.chopeks.movies.server.db.MovieCategories
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import pl.chopeks.movies.server.model.Category
import pl.chopeks.movies.server.model.CategoryPojo
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import pl.chopeks.movies.server.utils.urlImageToBase64

fun Route.categoryService() {
  get("/categories") { call.respond(transaction { Category.all().sortedBy { it.name }.map { it.pojo } }) }

  //region crud
  post("/category") {
    kotlin.runCatching { call.receiveNullable<CategoryPojo>() }.getOrNull()?.let {
      call.respond(HttpStatusCode.OK, transaction {
        if (it.image?.startsWith("http") == true) {
          it.image = it.image?.urlImageToBase64()
        }
        if (Category.find { CategoryTable.id eq it.id }.firstOrNull() != null) {
          CategoryTable.update({ CategoryTable.id eq it.id }) { obj ->
            obj[CategoryTable.name] = it.name
            obj[CategoryTable.image] = it.image
          }
        } else {
          CategoryTable.insert { new ->
            new[CategoryTable.name] = it.name
            new[CategoryTable.image] = it.image
          }
        }
        "{}"
      })
    }
  }
  delete("/category/{id}") {
    transaction {
      CategoryTable.deleteWhere { CategoryTable.id eq call.parameters["id"]!!.toInt() }
      MovieCategories.deleteWhere { MovieCategories.category eq call.parameters["id"]!!.toInt() }
    }
    call.respond(HttpStatusCode.OK, "{}")
  }
  //endregion

  //region binding
  post("/categories/{category}/{movie}") {
    val row = transaction {
      MovieCategories.selectAll().where { (MovieCategories.movie eq call.parameters["movie"]!!.toInt()) and (MovieCategories.category eq call.parameters["category"]!!.toInt()) }
        .firstOrNull()
    }
    if (row != null) {
      call.respond(HttpStatusCode.Conflict)
    } else {
      transaction {
        MovieCategories.insert {
          it[MovieCategories.movie] = call.parameters["movie"]!!.toInt()
          it[MovieCategories.category] = call.parameters["category"]!!.toInt()
        }
      }
      call.respond("{}")
    }
  }
  delete("/categories/{category}/{movie}") {
    transaction {
      MovieCategories.deleteWhere { (MovieCategories.movie eq call.parameters["movie"]!!.toInt()) and (MovieCategories.category eq call.parameters["category"]!!.toInt()) }
    }
    call.respond("{}")
  }
  //endregion
}