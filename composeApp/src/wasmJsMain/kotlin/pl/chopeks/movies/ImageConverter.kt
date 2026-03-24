package pl.chopeks.movies

import pl.chopeks.core.data.IImageConverter

class ImageConverter : IImageConverter {
	override fun urlToBase64(url: String, targetWidth: Int, targetHeight: Int): String? {
		return url
	}

	override fun makeScreenshot(path: String, permille: Long): ByteArray {
		return byteArrayOf()// todo - dummy method ok? does wasm need screenshots?
	}

	override fun makeBase64Screenshot(path: String, permille: Long): String? {
		return null // todo - dummy method ok? does wasm need screenshots?
	}
}