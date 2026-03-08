package pl.chopeks.movies

import pl.chopeks.core.IImageConverter
import pl.chopeks.movies.server.utils.urlImageToBase64

class ImageConverter: IImageConverter {
	override fun urlToBase64(url: String, targetWidth: Int, targetHeight: Int): String {
		return url.urlImageToBase64(targetWidth, targetHeight)
	}
}