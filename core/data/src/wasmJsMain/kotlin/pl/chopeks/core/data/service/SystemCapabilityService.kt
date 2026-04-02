package pl.chopeks.core.data.service

import pl.chopeks.core.data.utils.RpcWrapper

class SystemCapabilityService(
	private val delegate: ISystemCapabilityService,
): ISystemCapabilityService, RpcWrapper {
	override suspend fun discover() = rpc {
		delegate.discover()
	}
}