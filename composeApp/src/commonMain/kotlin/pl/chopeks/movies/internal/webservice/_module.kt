package pl.chopeks.movies.internal.webservice

import org.kodein.di.DI
import org.kodein.di.bindProvider
import org.kodein.di.instance

val webServiceModule; get() = DI.Module(name = "webService") {
  bindProvider { ActorsAPI(instance()) }
  bindProvider { CategoriesAPI(instance()) }
  bindProvider { VideosAPI(instance()) }
  bindProvider { DuplicatesAPI(instance()) }
  bindProvider { SettingsAPI(instance()) }
}