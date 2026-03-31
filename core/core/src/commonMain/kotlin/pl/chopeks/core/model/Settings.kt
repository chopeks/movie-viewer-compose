package pl.chopeks.core.model

import kotlinx.serialization.Serializable
@Serializable
data class Settings(
    val browser: String = "",
    val moviePlayer: String = "",
    val encoderSource: String = "",
    val encoderSink: String = ""
)

@Serializable
data class Path(
    val path: String,
    val count: Int
)