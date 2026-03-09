package pl.chopeks.core.database.datasource

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import pl.chopeks.core.database.CategoryTable
import pl.chopeks.core.database.MovieCategories
import pl.chopeks.core.database.model.CategoryEntity
import pl.chopeks.core.model.Category
import pl.chopeks.core.model.Video

class CategoriesDataSource(
	private val db: Database
) {
	suspend fun getCategories(): List<Category> {
		return withContext(Dispatchers.IO) {
			transaction(db) {
				CategoryEntity.all().sortedBy { it.name.lowercase() }.map { it.pojo }
			}
		}
	}

	suspend fun getImage(category: Category): String? = withContext(Dispatchers.IO) {
		transaction(db) {
			CategoryEntity.findById(category.id)?.image?.substringAfter(",")
		}
	}

	suspend fun bind(category: Category, video: Video) = withContext(Dispatchers.IO) {
		val row = transaction(db) {
			MovieCategories.selectAll().where { (MovieCategories.movie eq video.id) and (MovieCategories.category eq category.id) }
				.firstOrNull()
		}
		if (row == null) {
			transaction(db) {
				MovieCategories.insert {
					it[MovieCategories.movie] = video.id
					it[MovieCategories.category] = category.id
				}
			}
		}
	}

	suspend fun unbind(category: Category, video: Video) = withContext(Dispatchers.IO) {
		transaction(db) {
			MovieCategories.deleteWhere { (MovieCategories.movie eq video.id) and (MovieCategories.category eq category.id) }
		}
	}


	suspend fun add(name: String, url: String?) = withContext(Dispatchers.IO) {
		transaction(db) {
			CategoryTable.insert { new ->
				new[CategoryTable.name] = name
				new[CategoryTable.image] = url?.ifBlank { null }
			}
			Unit
		}
	}

	suspend fun edit(id: Int, name: String, url: String?) = withContext(Dispatchers.IO) {
		transaction(db) {
			if (CategoryEntity.find { CategoryTable.id eq id }.firstOrNull() != null) {
				CategoryTable.update({ CategoryTable.id eq id }) { obj ->
					obj[CategoryTable.name] = name
					obj[CategoryTable.image] = url?.ifBlank { null }
				}
			} else {
				CategoryTable.insert { new ->
					new[CategoryTable.name] = name
					new[CategoryTable.image] = url?.ifBlank { null }
				}
			}
		}
	}

	fun delete(category: Category) {
		transaction(db) {
			CategoryTable.deleteWhere { CategoryTable.id eq category.id }
			MovieCategories.deleteWhere { MovieCategories.category eq category.id }
		}
	}
}