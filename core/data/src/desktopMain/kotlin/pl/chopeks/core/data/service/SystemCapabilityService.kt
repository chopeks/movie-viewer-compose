package pl.chopeks.core.data.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pl.chopeks.core.data.repository.ISystemCapabilityRepository
import pl.chopeks.core.ffmpeg.FfmpegManager
import pl.chopeks.core.fpcalc.FpcalcManager
import pl.chopeks.core.model.capability.Capability
import pl.chopeks.core.model.capability.ExternalSoftware

class SystemCapabilityService(
	private val repository: ISystemCapabilityRepository,
	private val ffmpegManager: FfmpegManager,
	private val fpcalcManager: FpcalcManager,
): ISystemCapabilityService {
	override suspend fun discover() {
		handleFfmpeg()
		handleFfprobe()
		handleFpcalc()
	}

	private suspend fun handleFfmpeg() = withContext(Dispatchers.IO) {
		val ffmpegCapabilities = ffmpegManager.getFfmpegCapabilities()
		if (ffmpegCapabilities != null) {
			repository.addVersion(ExternalSoftware.FFMPEG, ffmpegCapabilities.version)

			if (ffmpegCapabilities.hasVmaf)
				repository.addCapability(Capability.VIDEO_VMAF)

			ffmpegManager.availableEncoders().forEach { encoder ->
				when (encoder) {
					"hevc_amf" -> repository.addCapability(Capability.HEVC_AMD)
					"hevc_nvenc" -> repository.addCapability(Capability.HEVC_NVIDIA)
					"hevc_qsv" -> repository.addCapability(Capability.HEVC_INTEL)
					"libx265" -> repository.addCapability(Capability.HEVC_SOFTWARE)
				}
			}
		}
	}

	private suspend fun handleFfprobe() {
		val ffprobeVersion = ffmpegManager.getFfprobeVersion()
		if (ffprobeVersion != null) {
			repository.addVersion(ExternalSoftware.FFPROBE, ffprobeVersion)
		}
	}

	private suspend fun handleFpcalc() {
		val fpcalcVersion = fpcalcManager.getFpcalcVersion()
		if (fpcalcVersion != null) {
			repository.addVersion(ExternalSoftware.FPCALC, fpcalcVersion)
			repository.addCapability(Capability.AUDIO_FINGERPRINT)
		}
	}
}