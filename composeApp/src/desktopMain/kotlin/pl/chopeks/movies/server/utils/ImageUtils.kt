package pl.chopeks.movies.server.utils

import okhttp3.OkHttpClient
import okhttp3.Request
import org.imgscalr.Scalr
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.random.Random

fun ClosedRange<Int>.random() =
	Random.Default.nextInt(endInclusive - start) + start

fun BufferedImage.normalizeImage() = this
	.let { Scalr.resize(it, Scalr.Method.QUALITY, 400, it.height * 400 / it.width) }
	.let {
		val newHeight = minOf(it.width * 9 / 16, it.height)
		val offset = (it.height - newHeight) / 2
		Scalr.crop(it, 0, offset, it.width, newHeight)
	}

@OptIn(ExperimentalEncodingApi::class)
fun String.urlImageToBase64(targetWidth: Int, targetHeight: Int): String {
	val imageBytes = OkHttpClient().newCall(Request.Builder().url(this).build()).execute().body?.bytes() ?: return ""
	val originalImage = ImageIO.read(imageBytes.inputStream())

	try {
		// Calculate aspect ratio and scale
		val aspectRatio = originalImage.width.toFloat() / originalImage.height
		val (scaledWidth, scaledHeight) = if (originalImage.width > originalImage.height) {
			targetWidth to (targetWidth / aspectRatio).toInt()  // Scale by width
		} else {
			(targetHeight * aspectRatio).toInt() to targetHeight  // Scale by height
		}

		// Ensure scaled dimensions don't go below target size
		val finalWidth = scaledWidth.coerceAtLeast(targetWidth)
		val finalHeight = scaledHeight.coerceAtLeast(targetHeight)

		val resizedImage = BufferedImage(finalWidth, finalHeight, BufferedImage.TYPE_INT_RGB).apply {
			createGraphics().apply {
				setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
				drawImage(originalImage, 0, 0, finalWidth, finalHeight, null)
				dispose()
			}
		}

		// Crop to target size
		val (cropX, cropY) = (finalWidth - targetWidth) / 2 to (finalHeight - targetHeight) / 2

		// Ensure the cropping coordinates are valid and within the image bounds
		val validCropX = cropX.coerceIn(0, finalWidth - targetWidth)
		val validCropY = cropY.coerceIn(0, finalHeight - targetHeight)

		val croppedImage = resizedImage.getSubimage(validCropX, validCropY, targetWidth, targetHeight)

		// Convert to Base64 with high-quality JPEG compression
		val byteArrayOutputStream = ByteArrayOutputStream()
		val imageWriter = ImageIO.getImageWritersByFormatName("png").next()
		val imageWriteParam = imageWriter.defaultWriteParam

		// Set compression quality to high (90-95)
		imageWriteParam.compressionMode = ImageWriteParam.MODE_EXPLICIT
		imageWriteParam.compressionQuality = 0.95f

		// Write image to ByteArrayOutputStream with specified quality
		imageWriter.output = javax.imageio.stream.MemoryCacheImageOutputStream(byteArrayOutputStream)
		imageWriter.write(null, javax.imageio.IIOImage(croppedImage, null, null), imageWriteParam)
		imageWriter.dispose()

		// Convert to Base64
		return byteArrayOutputStream.toByteArray().let { "data:image/jpg;base64,${it.let(Base64.Mime::encode)}" }
	} catch (e: Exception) {
		e.printStackTrace()
	}
	return ""
}
