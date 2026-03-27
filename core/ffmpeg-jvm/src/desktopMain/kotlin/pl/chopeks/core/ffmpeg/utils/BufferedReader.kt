package pl.chopeks.core.ffmpeg.utils

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.yield
import java.io.BufferedReader

suspend fun BufferedReader.forEachLineNonBlocking(
	block: suspend (String) -> Unit
) = coroutineScope {
	while (isActive) {
		if (ready()) {
			val line = readLine()
				?: break
			block(line)
		}
		yield()
	}
}
