package pl.chopeks.movies.internal.screenmodel

import org.kodein.di.DI
import org.kodein.di.bindProvider
import org.kodein.di.instance

val screenModelModule = DI.Module(name = "screenModel") {
	bindProvider { ActorsScreenModel(instance(), instance()) }
	bindProvider { CategoriesScreenModel(instance(), instance()) }
	bindProvider { VideosScreenModel(instance(), instance(), instance()) }
	bindProvider { DuplicatesScreenModel(instance(), instance()) }
	bindProvider { SettingsScreenModel(instance()) }
}