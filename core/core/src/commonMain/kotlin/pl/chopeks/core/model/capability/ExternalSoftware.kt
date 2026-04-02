package pl.chopeks.core.model.capability

enum class ExternalSoftware(val visibleName: String, val recommendedVersion: String) {
	FFMPEG("ffmpeg", "6.0"),
	FFPROBE("ffprobe", "6.0"),
	FPCALC("fpcalc", "1.6.0"),
}
