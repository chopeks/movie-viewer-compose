package pl.chopeks.core.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.rpc.annotations.Rpc

@Rpc
interface IEncoderRepository {
	fun observeEncodingStatus(): Flow<Map<String, Float>>
}