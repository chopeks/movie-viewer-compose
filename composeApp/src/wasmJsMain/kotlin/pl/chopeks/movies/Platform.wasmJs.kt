package pl.chopeks.movies

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.domDataTransferOrNull
import cafe.adriel.voyager.core.screen.Screen
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Uint8Array
import org.khronos.webgl.get
import org.w3c.files.FileReader
import pl.chopeks.movies.screen.ActorsScreen


class WasmPlatform : Platform {
	override val name: String = "Web with Kotlin/Wasm"

	@OptIn(ExperimentalComposeUiApi::class, ExperimentalWasmJsInterop::class)
	override fun getDragAndDropFiles(
		event: DragAndDropEvent,
		onFilesReady: (List<ByteArray>) -> Unit
	): Boolean {
		val dataTransfer = event.transferData?.domDataTransferOrNull
			?: return false

		val fileList = dataTransfer.files

		if (fileList.length == 0)
			return false

		val results = mutableListOf<ByteArray>()
		var processedCount = 0

		for (i in 0 until fileList.length) {
			val file = fileList.item(i)
				?: continue

			val reader = FileReader()

			reader.onload = {
				val uint8Array = Uint8Array(reader.result as ArrayBuffer)
				val bytes = ByteArray(uint8Array.length) { index -> uint8Array[index] }

				results.add(bytes)
				processedCount++

				if (processedCount == fileList.length)
					onFilesReady(results)
			}

			reader.onerror = {
				processedCount++
				if (processedCount == fileList.length)
					onFilesReady(results)
			}

			reader.readAsArrayBuffer(file)
		}
		return true
	}
}

actual fun getPlatform(): Platform = WasmPlatform()
actual fun getStartingScreen(): Screen = ActorsScreen()
