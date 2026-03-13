package pl.chopeks.core.data

import kotlinx.rpc.RpcClient
import kotlinx.rpc.withService
import org.kodein.di.DI
import org.kodein.di.bindProvider
import org.kodein.di.instance
import pl.chopeks.core.data.repository.*

val dataModule = DI.Module("data-di") {
	bindProvider<IActorRepository> { instance<RpcClient>().withService<IActorRepository>() }
	bindProvider<ISettingsRepository> { instance<RpcClient>().withService<ISettingsRepository>() }
	bindProvider<ICategoryRepository> { instance<RpcClient>().withService<ICategoryRepository>() }
	bindProvider<IVideoRepository> { instance<RpcClient>().withService<IVideoRepository>() }
	bindProvider<IDuplicateRepository> { instance<RpcClient>().withService<IDuplicateRepository>() }
}