package pl.chopeks.movies.screen

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.Navigator
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.compose.setSingletonImageLoaderFactory
import coil3.request.crossfade
import coil3.util.DebugLogger
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.compose.withDI
import pl.chopeks.movies.internal.screenmodel.screenModelModule
import pl.chopeks.movies.internal.webservice.webServiceModule

class HomeScreen(platformDI: DI) : Screen, DIAware {

  override val di = DI.lazy {
    extend(platformDI)
    import(screenModelModule)
    import(webServiceModule)
  }

  @Composable
  override fun Content() {
    withDI(di) {
      Navigator(ActorsScreen())
    }
  }
}