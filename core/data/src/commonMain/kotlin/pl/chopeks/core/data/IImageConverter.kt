package pl.chopeks.core.data

interface IImageConverter {
	suspend fun urlToBase64(url: String, targetWidth: Int, targetHeight: Int): String?
	suspend fun bytesToBase64(bytes: ByteArray, targetWidth: Int, targetHeight: Int): String?
	fun makeScreenshot(path: String, permille: Long = 110): ByteArray
	fun makeBase64Screenshot(path: String, permille: Long = 110): String?
}
