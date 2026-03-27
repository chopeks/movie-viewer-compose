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