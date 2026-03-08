package pl.chopeks.core.data

import org.kodein.di.DI
import org.kodein.di.bindProvider
import org.kodein.di.instance
import pl.chopeks.core.data.repository.ActorRepository
import pl.chopeks.core.data.repository.IActorRepository

val dataModule = DI.Module("data-di") {
	bindProvider<IActorRepository> { ActorRepository(instance()) }
}