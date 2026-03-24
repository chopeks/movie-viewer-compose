package pl.chopeks.core.data

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.kodein.di.DI
import org.kodein.di.bindSingleton
import org.kodein.di.instance
import pl.chopeks.core.data.repository.*
import pl.chopeks.core.database.databaseModule

actual fun bestConcurrencyDispatcher(): CoroutineDispatcher = Dispatchers.Default

val dataModule = DI.Module("data-di") {
	import(databaseModule)
	bindSingleton<IActorRepository> { ActorRepository(instance()) }
	bindSingleton<ISettingsRepository> { SettingsRepository(instance(), instance()) }
	bindSingleton<ICategoryRepository> { CategoryRepository(instance()) }
	bindSingleton<IVideoRepository> { VideoRepository(instance(), instance()) }
	bindSingleton<IDuplicateRepository> { DuplicateRepository(instance()) }
}