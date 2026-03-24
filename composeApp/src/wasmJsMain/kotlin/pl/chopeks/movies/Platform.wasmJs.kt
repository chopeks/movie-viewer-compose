package pl.chopeks.movies

import cafe.adriel.voyager.core.screen.Screen
import pl.chopeks.movies.screen.ActorsScreen


class WasmPlatform: Platform {
    override val name: String = "Web with Kotlin/Wasm"
}

actual fun getPlatform(): Platform = WasmPlatform()
actual fun getStartingScreen(): Screen = ActorsScreen()
