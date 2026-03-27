package pl.chopeks.core.model

import kotlinx.serialization.Serializable


@Serializable
data class EncodedVideo(
	val fileName: String,
	val progress: Float
) {
}