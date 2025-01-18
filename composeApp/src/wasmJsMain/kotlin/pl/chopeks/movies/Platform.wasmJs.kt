package pl.chopeks.movies

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

class WasmPlatform: Platform {
    override val name: String = "Web with Kotlin/Wasm"
}

actual fun getPlatform(): Platform = WasmPlatform()
actual fun bestConcurrencyDispatcher(): CoroutineDispatcher = Dispatchers.Unconfined
