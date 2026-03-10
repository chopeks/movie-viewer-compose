package pl.chopeks.movies.tasks.duplicates

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import pl.chopeks.core.database.MovieTable
import pl.chopeks.movies.server.utils.FpcalcUtils
import pl.chopeks.movies.server.utils.toByteArray
import pl.chopeks.movies.utils.AppLogger
import java.io.File
import kotlin.system.measureTimeMillis

object CollectFingerprintsUseCase {
	data class Fingerprint(val id: Int, val path: String, val fingerprint: ByteArray? = null)

	private val diskSemaphore = Semaphore(4)

	/**
	 * @return false if there are no more movies to be checked
	 * @return true if a movie was checked and is ready to check next
	 */
	suspend fun run(): Boolean = withContext(Dispatchers.Default) {
		var entries = withContext(Dispatchers.IO) {
			transaction {
				MovieTable.select(MovieTable.id, MovieTable.fingerprint, MovieTable.path)
					.where { MovieTable.fingerprint.isNull() }
					.limit(16)
					.map { Fingerprint(it[MovieTable.id].value, it[MovieTable.path]) }
			}
		}
		if (entries.isEmpty()) {
			AppLogger.log("No entries without fingerprint found, aborting...")
			return@withContext false
		}

		val time = measureTimeMillis {
			entries = entries.map {
				async {
					val file = File(it.path)
					val fingerprint = diskSemaphore.withPermit {
						FpcalcUtils.getFingerprint(file)
					}
					it.copy(fingerprint = fingerprint?.toByteArray())
				}
			}.awaitAll()
		}

		if (entries.isEmpty())
			return@withContext false

		val left = withContext(Dispatchers.IO) {
			transaction {
				entries.forEach { entry ->
					MovieTable.update({ MovieTable.id eq entry.id }) {
						it[MovieTable.fingerprint] = entry.fingerprint
					}
				}
				MovieTable.select(MovieTable.fingerprint).where { MovieTable.fingerprint.isNull() }.count()
			}
		}

		AppLogger.log("added fingerprint to ${entries.joinToString { it.id.toString(10) }} - todo: $left (took ${time/1000}s)")
		return@withContext true
	}
}