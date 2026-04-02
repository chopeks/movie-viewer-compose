package pl.chopeks.core.data.repository

import kotlinx.rpc.annotations.Rpc
import pl.chopeks.core.model.capability.Capability
import pl.chopeks.core.model.capability.ExternalSoftware

@Rpc
interface ISystemCapabilityRepository {
	suspend fun addCapability(capability: Capability)
	suspend fun hasCapability(capability: Capability): Boolean

	suspend fun addVersion(software: ExternalSoftware, version: String)
	suspend fun getVersion(software: ExternalSoftware): String?
}