package pl.chopeks.core.data.repository

import kotlinx.rpc.annotations.Rpc
import pl.chopeks.core.model.Actor
import pl.chopeks.core.model.Duplicates

@Rpc
interface IDuplicateRepository {
	suspend fun getCertainDuplicates(): List<Duplicates>
	suspend fun cancel(model: Duplicates)
	suspend fun count(): Int

	suspend fun deduplicate(actor: Actor)
	suspend fun deduplicateAll()
}