package pl.chopeks.core.database.duplicates

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import pl.chopeks.core.database.MovieTable

class FingerprintLocalDataSource(
	private val db: Database,
) {
	data class Fingerprint(
		val id: Int,
		val path: String,
		val duration: Int = 0,
		val fingerprint: ByteArray? = null,
		val needle: ByteArray? = null
	)

	suspend fun getVideosWithoutFingerprints(limit: Int = 16) = withContext(Dispatchers.IO) {
		transaction(db) {
			MovieTable.select(MovieTable.id, MovieTable.fingerprint, MovieTable.needle, MovieTable.path, MovieTable.duration)
				.where {
					MovieTable.fingerprint.isNull() or
						MovieTable.needle.isNull()
				}
				.orderBy(MovieTable.duration, SortOrder.ASC)
				.limit(limit)
				.map { Fingerprint(it[MovieTable.id].value, it[MovieTable.path], it[MovieTable.duration]!!, it[MovieTable.fingerprint], it[MovieTable.needle]) }
		}
	}

	suspend fun store(entries: List<Fingerprint>) = withContext(Dispatchers.IO) {
		transaction(db) {
			entries.forEach { entry ->
				MovieTable.update({ MovieTable.id eq entry.id }) {
					it[MovieTable.fingerprint] = entry.fingerprint
					it[MovieTable.needle] = entry.needle
				}
			}
		}
	}

	suspend fun countTodo() = withContext(Dispatchers.IO) {
		transaction(db) {
			MovieTable.select(MovieTable.fingerprint, MovieTable.needle)
				.where { MovieTable.fingerprint.isNull() or MovieTable.needle.isNull() }
				.count()
		}
	}
}