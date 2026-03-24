package pl.chopeks.usecase

import org.kodein.di.DI
import org.kodein.di.bindProvider
import org.kodein.di.instance
import pl.chopeks.usecase.video.GetDuplicatesUseCase

val useCaseModule = DI.Module("useCaseModule") {
	bindProvider { GetDuplicatesUseCase(instance(), instance()) }
}