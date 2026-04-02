package pl.chopeks.movies.mapping

import androidx.compose.runtime.Composable
import movieviewer.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import pl.chopeks.core.model.capability.Capability


@Composable
fun Capability.description(): String = when (this) {
	Capability.VIDEO_VMAF -> stringResource(Res.string.feature_desc_vmaf)
	Capability.HEVC_AMD -> stringResource(Res.string.feature_desc_hevc_amd)
	Capability.HEVC_NVIDIA -> stringResource(Res.string.feature_desc_hevc_nvidia)
	Capability.HEVC_INTEL -> stringResource(Res.string.feature_desc_hevc_intel)
	Capability.HEVC_SOFTWARE -> stringResource(Res.string.feature_desc_hevc_software)
	Capability.AUDIO_FINGERPRINT -> stringResource(Res.string.feature_desc_audio_fingerprint)
}