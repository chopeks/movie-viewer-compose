package pl.chopeks.core.ffmpeg

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import pl.chopeks.core.model.comparator.VideoCompareResult
import java.awt.image.BufferedImage
import java.io.File
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sqrt

class VideoComparator(
	private val ffmpeg: FfmpegManager
) {
	suspend fun compareVideos(
		originalFile: File,
		candidateFile: File,
		samples: Int = 4
	): VideoCompareResult = withContext(Dispatchers.Default) {
		val step = (1000 / (samples + 1))

		val results = (1..samples).map { i ->
			async {
				val permille = (i * step).toLong()

				val img1 = ffmpeg.getFrame(originalFile, permille, 480, 480)
				val img2 = ffmpeg.getFrame(candidateFile, permille, 480, 480)

				if (img1 == null || img2 == null)
					return@async null

				val psnr = calculatePSNR(img1, img2)
				val ssim = calculateSSIM(img1, img2)

				psnr to ssim
			}
		}.awaitAll()
			.filterNotNull()

		if (results.isEmpty())
			return@withContext VideoCompareResult(0.0, 0.0)

		VideoCompareResult(
			psnr = results.map { it.first }.average(),
			ssim = results.map { it.second }.average()
		)
	}

	fun calculatePSNR(img1: BufferedImage, img2: BufferedImage): Double {
		var mse = 0.0
		val w = img1.width
		val h = img1.height

		for (y in 0 until h) {
			for (x in 0 until w) {
				val rgb = img1.getRGB(x, y)
				val r1 = (rgb shr 16) and 0xff
				val g1 = (rgb shr 8) and 0xff
				val b1 = rgb and 0xff

				val rgb2 = img2.getRGB(x, y)
				val r2 = (rgb2 shr 16) and 0xff
				val g2 = (rgb2 shr 8) and 0xff
				val b2 = rgb2 and 0xff

				mse += (r1 - r2).toDouble().pow(2.0)
				mse += (g1 - g2).toDouble().pow(2.0)
				mse += (b1 - b2).toDouble().pow(2.0)
			}
		}
		mse /= (w * h * 3.0)

		if (mse == 0.0)
			return 100.0

		return 20.0 * log10(255.0 / sqrt(mse))
	}

	fun calculateSSIM(img1: BufferedImage, img2: BufferedImage): Double {
		val l1 = getLuminanceArray(img1)
		val l2 = getLuminanceArray(img2)
		val n = l1.size.toDouble()

		var mu1 = 0.0
		var mu2 = 0.0
		for (i in l1.indices) {
			mu1 += l1[i]
			mu2 += l2[i]
		}
		mu1 /= n
		mu2 /= n

		var sigma1Sq = 0.0
		var sigma2Sq = 0.0
		var sigma12 = 0.0
		for (i in l1.indices) {
			val dev1 = l1[i] - mu1
			val dev2 = l2[i] - mu2
			sigma1Sq += dev1 * dev1
			sigma2Sq += dev2 * dev2
			sigma12 += dev1 * dev2
		}
		sigma1Sq /= (n - 1)
		sigma2Sq /= (n - 1)
		sigma12 /= (n - 1)

		val c1 = 6.5025
		val c2 = 58.5225
		return ((2 * mu1 * mu2 + c1) * (2 * sigma12 + c2)) /
			((mu1 * mu1 + mu2 * mu2 + c1) * (sigma1Sq + sigma2Sq + c2))
	}

	private fun getLuminanceArray(img: BufferedImage): DoubleArray {
		val data = DoubleArray(img.width * img.height)
		var i = 0
		for (y in 0 until img.height) {
			for (x in 0 until img.width) {
				val rgb = img.getRGB(x, y)
				val r = (rgb shr 16) and 0xff
				val g = (rgb shr 8) and 0xff
				val b = rgb and 0xff
				data[i++] = 0.299 * r + 0.587 * g + 0.114 * b
			}
		}
		return data
	}
}