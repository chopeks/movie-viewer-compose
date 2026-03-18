package pl.chopeks.core.database

import io.kotest.core.listeners.TestListener
import io.kotest.core.spec.Spec
import org.jetbrains.exposed.sql.Database
import java.sql.Connection
import java.sql.DriverManager

class SqliteInMemoryListener : TestListener {
	private var keepAlive: Connection? = null
	private val URL = "jdbc:sqlite:file:testdb?mode=memory&cache=shared"

	lateinit var db: Database
		private set

	override suspend fun beforeSpec(spec: Spec) {
		keepAlive = DriverManager.getConnection(URL)
		db = DatabaseHelper.connect(url = URL)
	}

	override suspend fun afterSpec(spec: Spec) {
		keepAlive?.close()
	}
}