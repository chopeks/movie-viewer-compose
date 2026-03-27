package pl.chopeks.core.model

import kotlinx.serialization.Serializable

@Serializable
data class Settings(
	var browser: String,
	var moviePlayer: String,
	var encoderSource: String = "",
	var encoderSink: String = ""
)

@Serializable
data class Path(
	val path: String,
	val count: Int
)