package pl.chopeks.core.data

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.kodein.di.DI
import org.kodein.di.bindProvider
import org.kodein.di.instance
import pl.chopeks.core.data.repository.*
import pl.chopeks.core.database.databaseModule

actual fun bestConcurrencyDispatcher(): CoroutineDispatcher = Dispatchers.Default

val dataModule = DI.Module("data-di") {
	import(databaseModule)
	bindProvider<IActorRepository> { ActorRepository(instance()) }
	bindProvider<ISettingsRepository> { SettingsRepository(instance(), instance()) }
	bindProvider<ICategoryRepository> { CategoryRepository(instance()) }
	bindProvider<IVideoRepository> { VideoRepository(instance(), instance()) }
	bindProvider<IDuplicateRepository> { DuplicateRepository(instance()) }
}