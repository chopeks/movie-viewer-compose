package pl.chopeks.movies.utils

import org.jetbrains.skia.*
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi


@OptIn(ExperimentalEncodingApi::class)
fun ByteArray.imageBytesToBase64(targetWidth: Int, targetHeight: Int): String {
	val image = Image.makeFromEncoded(this)

	val scale = maxOf(
		targetWidth.toFloat() / image.width,
		targetHeight.toFloat() / image.height
	)

	val finalWidth = (image.width * scale).toInt()
	val finalHeight = (image.height * scale).toInt()

	val surface = Surface.makeRasterN32Premul(targetWidth, targetHeight)
	val canvas = surface.canvas

	val dx = (targetWidth - finalWidth) / 2f
	val dy = (targetHeight - finalHeight) / 2f

	val paint = Paint().apply {
		isAntiAlias = true
	}

	canvas.save()
	canvas.translate(dx, dy)
	canvas.drawImageRect(
		image,
		Rect.makeWH(image.width.toFloat(), image.height.toFloat()),
		Rect.makeWH(finalWidth.toFloat(), finalHeight.toFloat()),
		samplingMode = SamplingMode.MITCHELL,
		paint = paint,
		strict = true
	)
	canvas.restore()

	val encodedData = surface.makeImageSnapshot()
		.encodeToData(EncodedImageFormat.WEBP, 95)
		?: return ""

	val bytes = encodedData.bytes
	return "data:image/webp;base64,${Base64.Mime.encode(bytes)}"
}
