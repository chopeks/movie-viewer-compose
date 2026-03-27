package pl.chopeks.core.ffmpeg.utils

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.io.BufferedReader
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

suspend fun BufferedReader.forEachLineNonBlocking(
	timeout: Duration = 2.seconds,
	block: suspend (String) -> Unit
) = coroutineScope {
	var lastEmissionTime = System.currentTimeMillis()

	while (isActive) {
		if (ready()) {
			val line = readLine() ?: break // Stream closed naturally
			lastEmissionTime = System.currentTimeMillis()
			block(line)
		} else {
			val timeSinceLastLine = System.currentTimeMillis() - lastEmissionTime
			if (timeSinceLastLine > timeout.inWholeMilliseconds) {
				return@coroutineScope
			}
			delay(1)
		}
	}
}