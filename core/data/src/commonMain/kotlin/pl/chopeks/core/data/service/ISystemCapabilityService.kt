package pl.chopeks.core.data.service

import kotlinx.rpc.annotations.Rpc

@Rpc
interface ISystemCapabilityService {
	suspend fun discover()
}