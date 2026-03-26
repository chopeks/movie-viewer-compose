package pl.chopeks.core.model.comparator

import kotlinx.serialization.Serializable

@Serializable
data class VideoCompareResult(
	val ssim: Double,
	val psnr: Double,
) {
	val isValid: Boolean
		get() = (ssim > 0.80 && psnr > 20)

}