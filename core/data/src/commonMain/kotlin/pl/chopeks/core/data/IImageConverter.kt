package pl.chopeks.core.data

import pl.chopeks.core.model.IntRect

interface IImageConverter {
	suspend fun bytesToBase64(bytes: ByteArray, targetWidth: Int, targetHeight: Int, rect: IntRect): String?
	fun makeScreenshot(path: String, permille: Long = 110): ByteArray
	fun makeBase64Screenshot(path: String, permille: Long = 110): String?
}
