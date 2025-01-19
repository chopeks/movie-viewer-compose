package utils

import model.VideoCompareResult

object Python {
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
    val psnr = psnrRegex.find(output)?.groupValues?.get(1)?.toDouble()

    if (ssim != null && psnr != null) {
      return VideoCompareResult(ssim, psnr)
    }
    return null
  }
}