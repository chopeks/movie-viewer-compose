package pl.chopeks.movies.internal

import org.kodein.di.DI
import pl.chopeks.movies.internal.screenmodel.screenModelModule
import pl.chopeks.movies.internal.webservice.webServiceModule

val internalModule; get() = DI.Module("internal") {
  import(screenModelModule)
  import(webServiceModule)
}