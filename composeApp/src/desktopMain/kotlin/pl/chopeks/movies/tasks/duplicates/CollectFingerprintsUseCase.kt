package pl.chopeks.movies.tasks.duplicates

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import pl.chopeks.movies.server.db.MovieTable
import pl.chopeks.movies.server.utils.FpcalcUtils
import pl.chopeks.movies.server.utils.toByteArray
import pl.chopeks.movies.utils.AppLogger
import java.io.File

object CollectFingerprintsUseCase {
	private data class Fingerprint(val id: Int, val path: String, val fingerprint: ByteArray? = null)

	suspend fun run(scope: CoroutineScope, pool: ExecutorCoroutineDispatcher): Boolean = with(scope) {
		var entries = transaction {
			MovieTable.select(MovieTable.id, MovieTable.fingerprint, MovieTable.path)
				.where { MovieTable.fingerprint.isNull() }
				.limit(16)
				.map { Fingerprint(it[MovieTable.id].value, it[MovieTable.path]) }
		}
		if (entries.isEmpty())
			return false

		entries = entries.map {
			async(pool) {
				it.copy(fingerprint = FpcalcUtils.getFingerprint(File(it.path))?.toByteArray())
			}
		}.awaitAll()

		if (entries.isEmpty())
			return false

		val left = transaction {
			entries.forEach { entry ->
				MovieTable.update({ MovieTable.id eq entry.id }) {
					it[MovieTable.fingerprint] = entry.fingerprint
				}
			}
			MovieTable.select(MovieTable.fingerprint).where { MovieTable.fingerprint.isNull() }.count()
		}

		AppLogger.log("added fingerprint to ${entries.joinToString { it.id.toString(10) }} - todo: $left")
		return true
	}
}