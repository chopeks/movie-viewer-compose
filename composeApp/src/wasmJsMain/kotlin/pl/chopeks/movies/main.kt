package pl.chopeks.movies

import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.window.ComposeViewport
import cafe.adriel.voyager.navigator.Navigator
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.compose.setSingletonImageLoaderFactory
import coil3.memory.MemoryCache
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.CachePolicy
import coil3.request.crossfade
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.rpc.RpcClient
import kotlinx.rpc.krpc.ktor.client.installKrpc
import kotlinx.rpc.krpc.ktor.client.rpc
import kotlinx.rpc.krpc.ktor.client.rpcConfig
import kotlinx.rpc.krpc.serialization.json.json
import kotlinx.rpc.withService
import org.kodein.di.*
import org.w3c.dom.events.Event
import pl.chopeks.core.data.IImageConverter
import pl.chopeks.core.data.ITaskManager
import pl.chopeks.core.data.IVideoPlayer
import pl.chopeks.core.data.dataModule
import pl.chopeks.movies.screen.HomeScreen
import pl.chopeks.movies.utils.KeyEventManager

fun getAsyncImageLoader(context: PlatformContext) =
	ImageLoader.Builder(context)
		.crossfade(true)
		.memoryCachePolicy(CachePolicy.ENABLED).memoryCache {
			MemoryCache.Builder().maxSizeBytes(2147483648).strongReferencesEnabled(true).build()
		}
		.components {
			add(KtorNetworkFetcherFactory())
		}
		.build()


@OptIn(ExperimentalComposeUiApi::class)
fun main() {
	val di = DI.lazy {
		import(dataModule)
		bindProvider<IImageConverter> { ImageConverter() }
		bindSingleton { KeyEventManager() }
		bindProvider {
			HttpClient {
				installKrpc {
					serialization {
						json()
					}
				}
				expectSuccess = true
				install(ContentNegotiation) {
					json()
				}
			}
		}
		bindSingleton<RpcClient> {
			HttpClient { installKrpc() }.rpc {
				url("ws://localhost:15551/rpc")
				rpcConfig {
					serialization {
						json()
					}
				}
			}
		}
		bindProvider<IVideoPlayer> { instance<RpcClient>().withService<IVideoPlayer>() }
		bindProvider<ITaskManager> { instance<RpcClient>().withService<ITaskManager>() }
		// add platform specific stuff here
	}

	ComposeViewport(document.body!!) {
		DisposableEffect(Unit) {
			val keyListener: (Event) -> Unit = { event ->
				di.direct.instance<KeyEventManager>().propagateKeyEvent(KeyEvent(nativeKeyEvent = event))
			}
			window.addEventListener("keyup", keyListener)
			onDispose {
				window.removeEventListener("keyup", keyListener)
			}
		}
		setSingletonImageLoaderFactory { context ->
			getAsyncImageLoader(context)
		}
		MaterialTheme(colors = darkColors()) {
			Navigator(HomeScreen(di))
		}
	}
}