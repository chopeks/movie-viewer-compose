package pl.chopeks.core.data.repository

import pl.chopeks.core.database.datasource.DuplicateLocalDataSource
import pl.chopeks.core.model.Duplicates

class DuplicateRepository(
	private val dataSource: DuplicateLocalDataSource
) : IDuplicateRepository {
	override suspend fun getCertainDuplicates(): List<Duplicates> {
		return dataSource.getCertainDuplicates()
	}

	override suspend fun cancel(model: Duplicates) {
		return dataSource.cancel(model)
	}

	override suspend fun count(): Int {
		return dataSource.count()
	}

	override fun close() {
		/* no-op */
	}
}