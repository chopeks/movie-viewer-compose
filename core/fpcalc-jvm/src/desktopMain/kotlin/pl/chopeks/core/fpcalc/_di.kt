package pl.chopeks.core.fpcalc

import org.kodein.di.DI
import org.kodein.di.bindSingleton
import org.kodein.di.instance

val fpcalcModule = DI.Module("fpcalc") {
	bindSingleton { FpcalcManager(instance()) }
}