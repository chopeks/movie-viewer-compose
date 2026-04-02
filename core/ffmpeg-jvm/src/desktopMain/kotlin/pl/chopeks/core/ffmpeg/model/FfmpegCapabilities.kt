package pl.chopeks.core.ffmpeg.model

import kotlinx.serialization.Serializable

@Serializable
data class FfmpegCapabilities(
	val version: String,
	val hasVmaf: Boolean,
	val hasAmf: Boolean,
	val hasNvenc: Boolean,
	val hasLibx265: Boolean
)