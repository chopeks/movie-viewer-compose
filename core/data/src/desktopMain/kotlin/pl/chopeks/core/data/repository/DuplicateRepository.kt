package pl.chopeks.core.data.repository

import pl.chopeks.core.database.datasource.DuplicateLocalDataSource
import pl.chopeks.core.model.Actor
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

	override suspend fun deduplicate(actor: Actor) {
		return dataSource.deduplicate(actor)
	}

	override suspend fun deduplicateAll() {
		return dataSource.deduplicateAll()
	}
}