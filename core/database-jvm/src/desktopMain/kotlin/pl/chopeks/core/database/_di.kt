package pl.chopeks.core.database

import org.kodein.di.DI
import org.kodein.di.bindProvider
import org.kodein.di.bindSingleton
import org.kodein.di.instance
import pl.chopeks.core.database.datasource.ActorLocalDataSource

val databaseModule = DI.Module("databaseModule") {
	bindSingleton { DatabaseHelper.connect() }
	bindProvider { ActorLocalDataSource(instance()) }
}