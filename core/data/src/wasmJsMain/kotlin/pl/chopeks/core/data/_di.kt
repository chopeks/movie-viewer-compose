package pl.chopeks.core.data

import org.kodein.di.DI
import org.kodein.di.bindProvider
import org.kodein.di.instance
import pl.chopeks.core.data.repository.ActorRepository
import pl.chopeks.core.data.repository.CategoryRepository
import pl.chopeks.core.data.repository.IActorRepository
import pl.chopeks.core.data.repository.ICategoryRepository
import pl.chopeks.core.data.repository.ISettingsRepository
import pl.chopeks.core.data.repository.IVideoRepository
import pl.chopeks.core.data.repository.SettingsRepository
import pl.chopeks.core.data.repository.VideoRepository

val dataModule = DI.Module("data-di") {
	bindProvider<IActorRepository> { ActorRepository(instance()) }
	bindProvider<ISettingsRepository> { SettingsRepository(instance()) }
	bindProvider<ICategoryRepository> { CategoryRepository(instance()) }
	bindProvider<IVideoRepository> { VideoRepository(instance()) }
}