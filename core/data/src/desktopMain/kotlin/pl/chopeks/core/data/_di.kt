package pl.chopeks.core.data

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.kodein.di.DI
import org.kodein.di.bindProvider
import org.kodein.di.bindSingleton
import org.kodein.di.instance
import pl.chopeks.core.data.repository.*
import pl.chopeks.core.data.service.IVideoEncodingService
import pl.chopeks.core.data.service.VideoEncodingService
import pl.chopeks.core.database.databaseModule

actual fun bestConcurrencyDispatcher(): CoroutineDispatcher = Dispatchers.Default

val dataModule = DI.Module("data-di") {
	import(databaseModule)
	bindSingleton<IActorRepository> { ActorRepository(instance()) }
	bindSingleton<ISettingsRepository> { SettingsRepository(instance(), instance()) }
	bindSingleton<ICategoryRepository> { CategoryRepository(instance()) }
	bindSingleton<IVideoRepository> { VideoRepository(instance(), instance()) }
	bindSingleton<IDuplicateRepository> { DuplicateRepository(instance()) }
	bindSingleton<EncoderRepository> { EncoderRepository(instance(), instance(), instance(), instance()) }
	bindProvider<IEncoderRepository> { instance<EncoderRepository>() }

	// services
	bindSingleton<IVideoEncodingService> { VideoEncodingService(instance(), instance(), instance()) }
}