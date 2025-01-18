package pl.chopeks.movies

import androidx.compose.material.MaterialTheme
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import cafe.adriel.voyager.navigator.Navigator
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.compose.setSingletonImageLoaderFactory
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.CachePolicy
import coil3.request.crossfade
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import org.kodein.di.*
import pl.chopeks.movies.utils.KeyEventManager
import pl.chopeks.movies.screen.HomeScreen
import java.awt.Toolkit

fun getAsyncImageLoader(context: PlatformContext) =
  ImageLoader.Builder(context)
    .crossfade(true)
    .memoryCachePolicy(CachePolicy.ENABLED).memoryCache {
      MemoryCache.Builder().maxSizeBytes(2147483648).strongReferencesEnabled(true).build()
    }
    .components {
      add(OkHttpNetworkFetcherFactory(callFactory = { OkHttpClient() }))
    }
//    .logger(DebugLogger())
    .build()

fun main() = application {
  val screenSize = Toolkit.getDefaultToolkit().screenSize
  val density = LocalDensity.current.density
  val windowState = rememberWindowState(WindowPlacement.Maximized).apply {
    isMinimized = false
    size = DpSize(
      width = ((screenSize.width - 60) / density).dp,
      height = ((screenSize.height - 60) / density).dp
    )
  }
  val di = DI.lazy {
    bindSingleton { KeyEventManager() }
    bindProvider {
      HttpClient(OkHttp) {
        engine {
          config {
            followRedirects(true)
          }
        }
        expectSuccess = true
        install(ContentNegotiation) {
          json()
        }
        install(Logging)
      }
    }
    // add platform specific stuff here
  }

  Napier.base(DebugAntilog())

  setSingletonImageLoaderFactory { context ->
    getAsyncImageLoader(context)
  }

  val serverJob = Job()
  val serverScope = CoroutineScope(Dispatchers.IO + serverJob)

  serverScope.launch(Dispatchers.IO) {
//      embeddedServer(Netty, port = 15551) {
//        routing {
//          get("/") {
//            call.respondText("Hello from Ktor Server")
//          }
//          static("/assets") {
//            resources("assets")
//          }
//        }
//      }.start(wait = false)  // Start the server but don't block the thread
  }

  Window(
    onCloseRequest = {
      serverJob.cancel()
      serverScope.cancel()
      exitApplication()
    },
    state = windowState,
    title = "Movie Viewer",
    onKeyEvent = {
      di.direct.instance<KeyEventManager>().propagateKeyEvent(it)
    }
  ) {
    MaterialTheme {
      Navigator(HomeScreen(di))
    }
  }
}