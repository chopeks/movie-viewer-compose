package pl.chopeks.core.data

import org.kodein.di.DI
import org.kodein.di.bindProvider
import org.kodein.di.instance
import pl.chopeks.core.IVideoPlayer
import pl.chopeks.core.data.repository.*

val dataModule = DI.Module("data-di") {
	bindProvider<IActorRepository> { ActorRepository(instance()) }
	bindProvider<ISettingsRepository> { SettingsRepository(instance()) }
	bindProvider<ICategoryRepository> { CategoryRepository(instance()) }
	bindProvider<IVideoRepository> { VideoRepository(instance()) }
	bindProvider<IDuplicateRepository> { DuplicateRepository(instance()) }
	bindProvider<IVideoPlayer> { VideoRepository(instance()) }
}