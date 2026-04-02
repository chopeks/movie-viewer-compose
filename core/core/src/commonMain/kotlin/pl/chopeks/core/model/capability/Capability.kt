package pl.chopeks.core.model.capability

enum class Capability {
	VIDEO_VMAF,         // Visual Quality Measurement
	HEVC_AMD,           // AMD Hardware Encoding
	HEVC_NVIDIA,        // NVIDIA Hardware Encoding
	HEVC_INTEL,         // Intel Hardware Encoding
	HEVC_SOFTWARE,      // Software Encoding
	AUDIO_FINGERPRINT,  // Acoustid Audio Fingerprinting
}