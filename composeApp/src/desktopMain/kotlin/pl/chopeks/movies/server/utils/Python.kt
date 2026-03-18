package pl.chopeks.movies.server.utils

import pl.chopeks.movies.server.model.VideoCompareResult
import java.io.File
import java.security.MessageDigest

object Python {
	fun init() {
		with(File("scripts")) {
			if (!(exists() && isDirectory))
				mkdir()
			arrayOf(
				"compareVideos.py",
				"requirements.txt",
			).forEach { name ->
				val algorithm = "SHA-256"
				val targetFile = File(this, name)

				val resourceStream = Python::class.java.classLoader.getResourceAsStream("scripts/$name")
					?: return@forEach

				val resourceBytes = resourceStream.readBytes()
				val resourceHash = MessageDigest.getInstance(algorithm).digest(resourceBytes)

				val existingHash = if (targetFile.exists()) {
					MessageDigest.getInstance(algorithm).digest(targetFile.readBytes())
				} else null

				if (existingHash == null || !resourceHash.contentEquals(existingHash)) {
					targetFile.writeBytes(resourceBytes)
				}
			}
		}
	}

	fun call(script: String, vararg params: String): Pair<Int, String> {
		val output = StringBuilder()

		// Run the process and capture the result
		val result = runCatching {
			ProcessBuilder("python", script, *params)
				.apply { redirectErrorStream(true) }
				.start()
				.inputStream
				.bufferedReader()
				.use { it.readText() }
		}

		// Determine the exit code and output
		val exitCode = result.fold(
			onSuccess = {
				output.append(it)
				0
			},
			onFailure = {
				output.append("Error: ${it.message}")
				-1
			}
		)

		return exitCode to output.toString().trim()
	}

	fun compareVideos(path: String, otherPath: String): VideoCompareResult? {
		val (exitCode, output) = call("scripts/compareVideos.py", path, otherPath)
		if (exitCode != 0)
			return null

		val ssimRegex = """Average SSIM: ([\d\.]+)""".toRegex()
		val psnrRegex = """Average PSNR: ([\d\.]+)""".toRegex()

		val ssim = ssimRegex.find(output)?.groupValues?.get(1)?.toDouble()
			?: return null
		val psnr = psnrRegex.find(output)?.groupValues?.get(1)?.toDouble()
			?: return null

		return VideoCompareResult(ssim, psnr)
	}
}