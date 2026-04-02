package pl.chopeks.core.model.capability


enum class Capability {
	AUDIO_ENGINE,       // fpcalc
	AUDIO_FINGERPRINT,  // Acoustid Audio Fingerprinting
	VIDEO_ENGINE,       // ffmpeg
	VIDEO_VMAF,         // Visual Quality Measurement
	HEVC_AMD,           // AMD Hardware Encoding
	HEVC_NVIDIA,        // NVIDIA Hardware Encoding
	HEVC_INTEL,         // Intel Hardware Encoding
	HEVC_SOFTWARE,      // Software Encoding
}