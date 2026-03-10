package pl.chopeks.core.database.duplicates

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import pl.chopeks.core.database.MovieTable

class FingerprintLocalDataSource(
	private val db: Database,
) {
	data class Fingerprint(
		val id: Int,
		val path: String,
		val fingerprint: ByteArray? = null
	)

	suspend fun getVideosWithoutFingerprints(limit: Int = 16) = withContext(Dispatchers.IO) {
		transaction(db) {
			MovieTable.select(MovieTable.id, MovieTable.fingerprint, MovieTable.path)
				.where { MovieTable.fingerprint.isNull() }
				.limit(limit)
				.map { Fingerprint(it[MovieTable.id].value, it[MovieTable.path]) }
		}
	}
}