package pl.chopeks.movies.internal

import org.kodein.di.DI
import pl.chopeks.movies.internal.screenmodel.screenModelModule

val internalModule; get() = DI.Module("internal") {
  import(screenModelModule)
}