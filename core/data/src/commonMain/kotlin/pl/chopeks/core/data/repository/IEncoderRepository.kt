package pl.chopeks.core.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.rpc.annotations.Rpc
import pl.chopeks.core.model.EncodeStatus

@Rpc
interface IEncoderRepository {
	fun observeEncodingStatus(): Flow<Map<String, EncodeStatus>>
}