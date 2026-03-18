package pl.chopeks.movies.server.model

data class VideoCompareResult(
  val ssim: Double,
  val psnr: Double,
  val isValid: Boolean = (ssim > 0.80 && psnr > 20)
)
