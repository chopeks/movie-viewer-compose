package pl.chopeks.core.data

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.rpc.RpcClient
import kotlinx.rpc.withService
import org.kodein.di.DI
import org.kodein.di.bindProvider
import org.kodein.di.instance
import pl.chopeks.core.data.repository.*

actual fun bestConcurrencyDispatcher(): CoroutineDispatcher = Dispatchers.Unconfined

val dataModule = DI.Module("data-di") {
	bindProvider<IActorRepository> {
		ActorRepository(instance<RpcClient>().withService<IActorRepository>())
	}
	bindProvider<ISettingsRepository> {
		SettingsRepository(instance<RpcClient>().withService<ISettingsRepository>())
	}
	bindProvider<ICategoryRepository> {
		CategoryRepository(instance<RpcClient>().withService<ICategoryRepository>())
	}
	bindProvider<IVideoRepository> {
		VideoRepository(instance<RpcClient>().withService<IVideoRepository>())
	}
	bindProvider<IDuplicateRepository> {
		DuplicateRepository(instance<RpcClient>().withService<IDuplicateRepository>())
	}
}