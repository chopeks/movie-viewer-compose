package pl.chopeks.core.data.repository

import pl.chopeks.core.model.capability.Capability
import pl.chopeks.core.model.capability.ExternalSoftware

class SystemCapabilityRepository : ISystemCapabilityRepository {
	private val versionMap = mutableMapOf<ExternalSoftware, String>()
	private val capabilitySet = mutableSetOf<Capability>()

	override suspend fun addCapability(capability: Capability) {
		capabilitySet.add(capability)
	}

	override suspend fun hasCapability(capability: Capability): Boolean {
		return capabilitySet.contains(capability)
	}

	override suspend fun addVersion(software: ExternalSoftware, version: String) {
		versionMap[software] = version
	}

	override suspend fun getVersion(software: ExternalSoftware): String? {
		return versionMap[software]
	}
}