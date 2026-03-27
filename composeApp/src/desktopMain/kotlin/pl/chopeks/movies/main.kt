package pl.chopeks.movies

import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
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
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import org.kodein.di.*
import pl.chopeks.core.data.IImageConverter
import pl.chopeks.core.data.IVideoPlayer
import pl.chopeks.core.data.dataModule
import pl.chopeks.movies.platform.ImageConverter
import pl.chopeks.movies.platform.VideoPlayer
import pl.chopeks.movies.screen.HomeScreen
import pl.chopeks.movies.screen.platformScreenModule
import pl.chopeks.movies.tasks.TaskManager
import pl.chopeks.movies.tasks.taskModule
import pl.chopeks.movies.utils.KeyEventManager
import java.awt.Toolkit

object BGTasks {
	val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
	val serverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
}

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

@OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
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
		import(dataModule)
		import(taskModule)
		import(platformScreenModule)
		bindProvider<CoroutineScope> { BGTasks.scope }
		bindProvider<IImageConverter> { ImageConverter(instance()) }
		bindProvider<IVideoPlayer> { VideoPlayer(instance(), instance()) }
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

	BGTasks.serverScope.launch {
		embeddedServer(Netty, port = 15551, module = { module(di) }).start(wait = true)
	}

	Window(
		onCloseRequest = {
			di.direct.instance<TaskManager>().cancel()
			runBlocking {
				listOf(BGTasks.scope, BGTasks.serverScope).forEach { scope ->
					scope.cancel()
					withTimeoutOrNull(500) {
						scope.coroutineContext.job.join()
					}
				}
			}
			exitApplication()
		},
		state = windowState,
		title = "Movie Viewer",
		onKeyEvent = {
			di.direct.instance<KeyEventManager>().propagateKeyEvent(it)
		}
	) {
		MaterialTheme(colors = darkColors()) {
			Navigator(HomeScreen(di))
		}
	}
}
