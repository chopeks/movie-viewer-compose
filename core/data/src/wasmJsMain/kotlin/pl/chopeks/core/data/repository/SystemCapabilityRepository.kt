package pl.chopeks.core.data.repository

import pl.chopeks.core.data.utils.RpcWrapper
import pl.chopeks.core.model.capability.Capability
import pl.chopeks.core.model.capability.ExternalSoftware

class SystemCapabilityRepository(
	private val delegate: ISystemCapabilityRepository
) : ISystemCapabilityRepository, RpcWrapper {
	override suspend fun addCapability(capability: Capability)= rpc  {
		delegate.addCapability(capability)
	}

	override suspend fun hasCapability(capability: Capability): Boolean = rpc {
		delegate.hasCapability(capability)
	}

	override suspend fun addVersion(software: ExternalSoftware, version: String) = rpc {
		delegate.addVersion(software, version)
	}

	override suspend fun getVersion(software: ExternalSoftware): String? = rpc {
		delegate.getVersion(software)

	}
}