package pl.chopeks.movies

import pl.chopeks.core.data.IImageConverter
import pl.chopeks.core.model.IntRect
import pl.chopeks.movies.utils.imageBytesToBase64

class ImageConverter : IImageConverter {
	override suspend fun bytesToBase64(bytes: ByteArray, targetWidth: Int, targetHeight: Int, rect: IntRect): String? {
		return try {
			bytes.imageBytesToBase64(targetWidth, targetHeight, rect)
		} catch (e: Throwable) {
			e.printStackTrace()
			null
		}
	}

	override fun makeScreenshot(path: String, permille: Long): ByteArray {
		return byteArrayOf()// todo - dummy method ok? does wasm need screenshots?
	}

	override fun makeBase64Screenshot(path: String, permille: Long): String? {
		return null // todo - dummy method ok? does wasm need screenshots?
	}
}