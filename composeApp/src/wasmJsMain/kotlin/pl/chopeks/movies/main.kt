package pl.chopeks.movies

import androidx.compose.material.MaterialTheme
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import cafe.adriel.voyager.navigator.Navigator
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.compose.setSingletonImageLoaderFactory
import coil3.memory.MemoryCache
import coil3.request.CachePolicy
import coil3.request.crossfade
import coil3.util.DebugLogger
import io.ktor.client.*
import io.ktor.client.engine.js.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.browser.document
import org.kodein.di.DI
import org.kodein.di.bindProvider
import org.kodein.di.bindSingleton
import pl.chopeks.movies.screen.HomeScreen

fun getAsyncImageLoader(context: PlatformContext) =
    ImageLoader.Builder(context)
        .crossfade(true)
        .memoryCachePolicy(CachePolicy.ENABLED).memoryCache {
            MemoryCache.Builder().maxSizeBytes(2147483648).strongReferencesEnabled(true).build()
        }
        .build()

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val di = DI.lazy {
        bindProvider {
            HttpClient(Js) {
                expectSuccess = true
                install(ContentNegotiation) {
                    json()
                }
            }
        }
        // add platform specific stuff here
    }

    ComposeViewport(document.body!!) {
        setSingletonImageLoaderFactory { context ->
            getAsyncImageLoader(context)
        }
        MaterialTheme {
            Navigator(HomeScreen(di))
        }
    }
}