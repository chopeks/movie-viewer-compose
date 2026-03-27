package pl.chopeks.core.ffmpeg.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import java.io.BufferedReader
import java.io.IOException

fun BufferedReader.linesFlow(): Flow<String> = callbackFlow {
	val job = launch(Dispatchers.IO) {
		try {
			while (isActive) {
				val line = readLine()
					?: break
				trySend(line)
				yield()
			}
		} catch (e: IOException) {
			// stream closed
		} finally {
			close()
		}
	}

	awaitClose {
		job.cancel()
		close()
	}
}