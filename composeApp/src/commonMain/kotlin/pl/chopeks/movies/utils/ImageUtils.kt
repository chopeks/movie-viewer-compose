package pl.chopeks.movies.utils

import org.jetbrains.skia.*
import pl.chopeks.core.model.IntRect
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi


/**
 * Main entry point: Takes bytes, crops/scales them, and returns a Base64 string.
 */
@OptIn(ExperimentalEncodingApi::class)
fun ByteArray.imageBytesToBase64(
	targetWidth: Int,
	targetHeight: Int,
	cropRect: IntRect
): String = Image.makeFromEncoded(this).let { image ->
	createProcessedImage(image, targetWidth, targetHeight, cropRect)
		.encodeToData(EncodedImageFormat.WEBP, 95)
		?.let { "data:image/webp;base64,${Base64.Mime.encode(it.bytes)}" }
		?: ""
}

/**
 * Handles the actual Skia drawing: Maps the source Rect to the destination Canvas.
 */
private fun createProcessedImage(
	sourceImage: Image,
	targetWidth: Int,
	targetHeight: Int,
	cropRect: IntRect
): Image = with(Surface.makeRasterN32Premul(targetWidth, targetHeight)) {
	canvas.drawImageRect(
		sourceImage,
		src = Rect.makeLTRB(cropRect.left.toFloat(), cropRect.top.toFloat(), cropRect.right.toFloat(), cropRect.bottom.toFloat()),
		dst = Rect.makeWH(targetWidth.toFloat(), targetHeight.toFloat()),
		SamplingMode.MITCHELL,
		Paint().apply { isAntiAlias = true },
		strict = true
	)
	makeImageSnapshot()
}
