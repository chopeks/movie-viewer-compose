package pl.chopeks.movies

import pl.chopeks.core.IImageConverter

class ImageConverter: IImageConverter {
	override fun urlToBase64(url: String, targetWidth: Int, targetHeight: Int): String? {
		return url
	}
}