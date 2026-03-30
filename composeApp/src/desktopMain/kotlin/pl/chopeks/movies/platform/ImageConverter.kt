package pl.chopeks.movies.platform

import org.jetbrains.skia.*
import pl.chopeks.core.data.IImageConverter
import pl.chopeks.core.ffmpeg.FfmpegManager
import pl.chopeks.movies.utils.imageBytesToBase64
import java.io.File
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class ImageConverter(
	private val ffmpegManager: FfmpegManager
) : IImageConverter {
	override suspend fun bytesToBase64(bytes: ByteArray, targetWidth: Int, targetHeight: Int): String? {
		return try {
			bytes.imageBytesToBase64(targetWidth, targetHeight)
		} catch (e: Throwable) {
			e.printStackTrace()
			null
		}
	}

	override fun makeScreenshot(path: String, permille: Long): ByteArray {
		return ffmpegManager.makeScreenshot(File(path), permille)
	}

	@OptIn(ExperimentalEncodingApi::class)
	override fun makeBase64Screenshot(path: String, permille: Long): String? {
		val screenshotBytes = makeScreenshot(path, permille)
		if (screenshotBytes.isEmpty()) return null

		try {
			val original = Image.makeFromEncoded(screenshotBytes)

			val targetWidth = 400
			val targetHeight = (targetWidth * 9) / 16

			val surface = Surface.makeRasterN32Premul(targetWidth, targetHeight)
			val canvas = surface.canvas

			val scale = targetWidth.toFloat() / original.width
			val scaledHeight = original.height * scale

			val dy = (targetHeight - scaledHeight) / 2f

			val destRect = Rect.makeLTRB(
				0f,
				dy,
				targetWidth.toFloat(),
				dy + scaledHeight
			)

			canvas.drawImageRect(
				image = original,
				src = Rect.makeWH(original.width.toFloat(), original.height.toFloat()),
				dst = destRect,
				samplingMode = SamplingMode.MITCHELL,
				paint = Paint().apply { isAntiAlias = true },
				strict = true
			)

			val encodedData = surface.makeImageSnapshot()
				.encodeToData(EncodedImageFormat.WEBP, 95)
				?: return null

			val base64Body = Base64.Mime.encode(encodedData.bytes)
			return "data:image/webp;base64,$base64Body"

		} catch (e: Exception) {
			e.printStackTrace()
			return null
		}
	}
}