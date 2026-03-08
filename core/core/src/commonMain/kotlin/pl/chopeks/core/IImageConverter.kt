package pl.chopeks.core

interface IImageConverter {
	fun urlToBase64(url: String, targetWidth: Int, targetHeight: Int): String
}