package pl.chopeks.movies

import androidx.compose.ui.draganddrop.DragAndDropEvent
import cafe.adriel.voyager.core.screen.Screen


interface Platform {
	val name: String
	fun getDragAndDropFiles(event: DragAndDropEvent, onFilesReady: (List<ByteArray>) -> Unit): Boolean
}

expect fun getPlatform(): Platform
expect fun getStartingScreen(): Screen
