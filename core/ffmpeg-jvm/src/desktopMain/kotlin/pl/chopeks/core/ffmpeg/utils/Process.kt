package pl.chopeks.core.ffmpeg.utils

import kotlinx.coroutines.coroutineScope

suspend fun Process.runCancellable(
	block: suspend Process.() -> Unit
) = coroutineScope {
	val process = this@runCancellable
	try {
		block(process)
		process.waitFor()
	} finally {
		if (process.isAlive) {
			ProcessHandle.of(process.pid()).ifPresent { handle ->
				handle.descendants().forEach { it.destroyForcibly() }
				handle.destroyForcibly()
			}
		}
	}
}