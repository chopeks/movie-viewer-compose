package pl.chopeks.movies.server.utils

@OptIn(ExperimentalUnsignedTypes::class)
fun UIntArray.toByteArray(): ByteArray {
	val bytes = ByteArray(size * 4)
	for (i in indices) {
		val v = this[i].toInt()
		bytes[i * 4 + 0] = (v shr 24).toByte()
		bytes[i * 4 + 1] = (v shr 16).toByte()
		bytes[i * 4 + 2] = (v shr 8).toByte()
		bytes[i * 4 + 3] = v.toByte()
	}
	return bytes
}

@OptIn(ExperimentalUnsignedTypes::class)
fun ByteArray.toUIntArray(): UIntArray {
	val result = UIntArray(size / 4)
	for (i in result.indices) {
		val b0 = (this[i * 4 + 0].toInt() and 0xff) shl 24
		val b1 = (this[i * 4 + 1].toInt() and 0xff) shl 16
		val b2 = (this[i * 4 + 2].toInt() and 0xff) shl 8
		val b3 = (this[i * 4 + 3].toInt() and 0xff)
		result[i] = (b0 or b1 or b2 or b3).toUInt()
	}
	return result
}