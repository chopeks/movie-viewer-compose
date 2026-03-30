package pl.chopeks.movies.utils

import kotlinx.browser.window
import kotlinx.coroutines.await
import org.khronos.webgl.Uint8Array
import org.khronos.webgl.set
import org.w3c.dom.*
import org.w3c.fetch.Response
import org.w3c.files.Blob
import org.w3c.files.BlobPropertyBag


@OptIn(ExperimentalWasmJsInterop::class)
private fun <T : JsAny?> jsArrayOf(): JsArray<T> = JsArray()

@OptIn(ExperimentalWasmJsInterop::class)
suspend fun String.urlImageToBase64(targetWidth: Int, targetHeight: Int): String {
	val response = window.fetch(this).await<Response>()
	val blob = response.blob().await<Blob>()
	return blob.blobToBase64(targetWidth, targetHeight)
}

@OptIn(ExperimentalWasmJsInterop::class)
suspend fun ByteArray.imageBytesToBase64(targetWidth: Int, targetHeight: Int): String {
	val uint8Array = Uint8Array(this.size)
	for (i in this.indices) {
		uint8Array[i] = this[i]
	}
	val jsArray = jsArrayOf<JsAny?>()
	jsArray[0] = uint8Array
	val blob = Blob(jsArray, BlobPropertyBag(type = "image/jpeg"))
	return blob.blobToBase64(targetWidth, targetHeight)
}

@OptIn(ExperimentalWasmJsInterop::class)
suspend fun Blob.blobToBase64(targetWidth: Int, targetHeight: Int): String {
	val bitmap = window.createImageBitmap(this).await<ImageBitmap>()

	val originalWidth = bitmap.width.toDouble()
	val originalHeight = bitmap.height.toDouble()

	val scale = maxOf(targetWidth / originalWidth, targetHeight / originalHeight)
	val drawWidth = originalWidth * scale
	val drawHeight = originalHeight * scale

	val offsetX = (targetWidth - drawWidth) / 2
	val offsetY = (targetHeight - drawHeight) / 2

	val canvas = window.document.createElement("canvas") as HTMLCanvasElement
	canvas.width = targetWidth
	canvas.height = targetHeight

	val ctx = canvas.getContext("2d") as CanvasRenderingContext2D

	ctx.imageSmoothingEnabled = true
	ctx.imageSmoothingQuality = ImageSmoothingQuality.HIGH

	ctx.drawImage(bitmap, offsetX, offsetY, drawWidth, drawHeight)

	return canvas.toDataURL("image/jpeg")
}
