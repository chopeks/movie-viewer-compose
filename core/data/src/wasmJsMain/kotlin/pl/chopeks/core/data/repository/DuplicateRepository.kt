package pl.chopeks.core.data.repository

import pl.chopeks.core.data.utils.RpcWrapper
import pl.chopeks.core.model.Actor
import pl.chopeks.core.model.Duplicates

class DuplicateRepository(
	private val deletage: IDuplicateRepository
) : IDuplicateRepository, RpcWrapper {
	override suspend fun getCertainDuplicates(): List<Duplicates> = rpc {
		deletage.getCertainDuplicates()
	}

	override suspend fun cancel(model: Duplicates) = rpc {
		deletage.cancel(model)
	}

	override suspend fun count(): Int = rpc {
		deletage.count()
	}

	override suspend fun deduplicate(actor: Actor) = rpc {
		deletage.deduplicate(actor)
	}

	override suspend fun deduplicateAll() = rpc {
		deletage.deduplicateAll()
	}
}