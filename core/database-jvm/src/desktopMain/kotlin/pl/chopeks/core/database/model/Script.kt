package pl.chopeks.core.database.model

data class VideoCompareResult(
  val ssim: Double,
  val psnr: Double,
  val isValid: Boolean = (ssim > 0.80 && psnr > 20)
)

data class AudioCompareResult(
  val confidence: Double,
  val elapsed: Double,
  val isValid: Boolean = confidence > 0.9
)