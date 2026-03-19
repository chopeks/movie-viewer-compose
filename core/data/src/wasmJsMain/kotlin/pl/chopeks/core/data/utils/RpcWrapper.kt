package pl.chopeks.core.data.utils

import io.ktor.client.plugins.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.retry

interface RpcWrapper {
	suspend fun <T> rpc(
		times: Int = 3,
		block: suspend () -> T
	): T {
		var lastException: Throwable? = null
		repeat(times) {
			try {
				return block()
			} catch (e: Exception) {
				lastException = e
				if (e !is ResponseException) throw mapToDomainException(e)
			}
		}
		throw mapToDomainException(lastException ?: RuntimeException("Unknown Error"))
	}

	fun <T> Flow<T>.rpc(): Flow<T> = this
		.retry(3) { e -> e is ResponseException }
		.catch { throw mapToDomainException(it) }

	fun mapToDomainException(e: Throwable): Throwable {
		return when (e) {
			is HttpRequestTimeoutException -> RuntimeException("Server unreachable")
			is ClientRequestException -> {
				if (e.response.status.value == 401) RuntimeException()
				else e
			}

			else -> e
		}
	}
}