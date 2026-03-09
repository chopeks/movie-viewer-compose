package pl.chopeks.movies

import pl.chopeks.core.IImageConverter
import pl.chopeks.movies.server.utils.urlImageToBase64

class ImageConverter : IImageConverter {
	override fun urlToBase64(url: String, targetWidth: Int, targetHeight: Int): String? {
		return try {
			url.urlImageToBase64(targetWidth, targetHeight)
		} catch (e: Throwable) {
			e.printStackTrace()
			null
		}
	}
}