package pl.chopeks.movies

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragData
import androidx.compose.ui.draganddrop.dragData
import cafe.adriel.voyager.core.screen.Screen
import pl.chopeks.movies.screen.PreloadScreen
import java.io.File
import java.net.URI

class JVMPlatform : Platform {
	override val name: String = "Java ${System.getProperty("java.version")}"

	@OptIn(ExperimentalComposeUiApi::class)
	override fun getDragAndDropFiles(
		event: DragAndDropEvent,
		onFilesReady: (List<ByteArray>) -> Unit
	): Boolean {
		val data = event.dragData()
		if (data is DragData.FilesList) {
			val files = data.readFiles().map {
				File(URI.create(it)).readBytes()
			}
			onFilesReady(files)
			return true
		}
		return false
	}
}

actual fun getPlatform(): Platform = JVMPlatform()
actual fun getStartingScreen(): Screen = PreloadScreen()
