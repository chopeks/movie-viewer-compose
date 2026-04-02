package pl.chopeks.core.model.capability

enum class ExternalSoftware(val visibleName: String, val recommendedVersion: String) {
	FFMPEG("ffmpeg", "8.1"),
	FFPROBE("ffprobe", "8.1"),
	FPCALC("fpcalc", "1.6.0"),
}
