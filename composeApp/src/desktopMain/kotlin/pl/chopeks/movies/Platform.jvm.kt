package pl.chopeks.movies

import cafe.adriel.voyager.core.screen.Screen
import pl.chopeks.movies.screen.PreloadScreen


class JVMPlatform : Platform {
  override val name: String = "Java ${System.getProperty("java.version")}"
}

actual fun getPlatform(): Platform = JVMPlatform()
actual fun getStartingScreen(): Screen = PreloadScreen()
