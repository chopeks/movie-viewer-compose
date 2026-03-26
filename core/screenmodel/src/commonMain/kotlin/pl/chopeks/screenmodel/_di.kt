package pl.chopeks.screenmodel

import org.kodein.di.DI
import org.kodein.di.bindProvider
import org.kodein.di.instance
import pl.chopeks.usecase.useCaseModule

val screenModelModule = DI.Module(name = "screenModel") {
	import(useCaseModule)
	bindProvider { ActorsScreenModel(instance(), instance(), instance()) }
	bindProvider { CategoriesScreenModel(instance(), instance()) }
	bindProvider { VideosScreenModel(instance(), instance(), instance(), instance(), instance(), instance()) }
	bindProvider { DuplicatesScreenModel(instance(), instance(), instance(), instance()) }
	bindProvider { SettingsScreenModel(instance()) }
}