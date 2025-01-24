package pl.chopeks.movies.internal.screenmodel

import org.kodein.di.DI
import org.kodein.di.bindProvider
import org.kodein.di.instance

val screenModelModule; get() = DI.Module(name = "screenModel") {
  bindProvider { ActorsScreenModel(instance()) }
  bindProvider { CategoriesScreenModel(instance()) }
  bindProvider { VideosScreenModel(instance(), instance(), instance()) }
  bindProvider { DuplicatesScreenModel(instance(), instance()) }
  bindProvider { SettingsScreenModel(instance()) }
}