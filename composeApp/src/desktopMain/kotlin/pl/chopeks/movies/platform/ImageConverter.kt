package pl.chopeks.movies.platform

import com.chopeks.pl.chopeks.core.ffmpeg.FfmpegManager
import pl.chopeks.core.IImageConverter
import pl.chopeks.movies.server.utils.normalizeImage
import pl.chopeks.movies.server.utils.urlImageToBase64
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.*
import javax.imageio.ImageIO

class ImageConverter(
	private val ffmpegManager: FfmpegManager
) : IImageConverter {
	override fun urlToBase64(url: String, targetWidth: Int, targetHeight: Int): String? {
		return try {
			url.urlImageToBase64(targetWidth, targetHeight)
		} catch (e: Throwable) {
			e.printStackTrace()
			null
		}
	}

	override fun makeScreenshot(path: String, permille: Long): ByteArray {
		return ffmpegManager.makeScreenshot(File(path), permille)
	}

	override fun makeBase64Screenshot(path: String, permille: Long): String? {
		val img = makeScreenshot(path, permille)
		if (img.isEmpty())
			return null
		val bytes = ImageIO.read(img.inputStream())
			.normalizeImage()
			.let {
				ByteArrayOutputStream().use { os ->
					ImageIO.write(it, "jpg", os)
					os.toByteArray()
				}
			}
		return "data:image/jpg;base64," + String(Base64.getMimeEncoder().encode(bytes))
	}
}