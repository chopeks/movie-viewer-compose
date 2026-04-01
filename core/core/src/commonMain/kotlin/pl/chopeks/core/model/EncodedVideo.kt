package pl.chopeks.core.model

import kotlinx.serialization.Serializable
import kotlin.time.Duration


@Serializable
data class EncodedVideo(
	val fileName: String,
	val progress: Float
) {
}

@Serializable
sealed class EncodeStatus {
	object Waiting : EncodeStatus()
	object Finished : EncodeStatus()
	object FinishedAndRemoved : EncodeStatus()
	data class Processing(val progress: Float, val eta: Duration? = null) : EncodeStatus()
	data class Error(val message: String) : EncodeStatus()
}

fun EncodeStatus.priority(): Int = when (this) {
	is EncodeStatus.Processing -> 0
	is EncodeStatus.Waiting -> 1
	is EncodeStatus.Finished -> 3
	is EncodeStatus.FinishedAndRemoved -> 4
	is EncodeStatus.Error -> 2
}