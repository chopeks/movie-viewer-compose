package pl.chopeks.movies.screen

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.Navigator
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.compose.withDI
import pl.chopeks.movies.getStartingScreen
import pl.chopeks.screenmodel.screenModelModule

class HomeScreen(platformDI: DI) : Screen, DIAware {
	override val di = DI.lazy {
		extend(platformDI)
		import(screenModelModule)
	}

	@Composable
	override fun Content() {
		withDI(di) {
			Navigator(getStartingScreen())
		}
	}
}