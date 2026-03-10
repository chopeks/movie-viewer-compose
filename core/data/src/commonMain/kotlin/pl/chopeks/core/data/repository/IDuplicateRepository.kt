package pl.chopeks.core.data.repository

import pl.chopeks.core.model.Actor
import pl.chopeks.core.model.Duplicates

interface IDuplicateRepository : AutoCloseable {
	suspend fun getCertainDuplicates(): List<Duplicates>
	suspend fun cancel(model: Duplicates)
	suspend fun count(): Int

	suspend fun deduplicate(actor: Actor)
	suspend fun deduplicateAll()
}