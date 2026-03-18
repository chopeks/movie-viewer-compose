package pl.chopeks.core.database

import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.jetbrains.exposed.sql.exists
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

class DatabaseHelperTest : FunSpec({
	val sqlite = SqliteInMemoryListener()

	extension(sqlite)

	test("check all tables generated") {
		transaction(sqlite.db) {
			assertSoftly {
				SchemaVerionsTable.exists() shouldBe true
				SchemaVerionsTable.selectAll().first()[SchemaVerionsTable.version] shouldBe 6

				CategoryTable.exists() shouldBe true
				ActorTable.exists() shouldBe true
				MovieTable.exists() shouldBe true
				MovieCategories.exists() shouldBe true
				MovieActors.exists() shouldBe true
				PathsTable.exists() shouldBe true
				MoviesToBeCheckedTable.exists() shouldBe true
				AudioToBeCheckedTable.exists() shouldBe true
				DetectedDuplicatesTable.exists() shouldBe true
			}
		}
	}

	test("check cleanUp") {
		transaction(sqlite.db) {
			assertSoftly {
				MovieTable.selectAll().count() shouldBe 0
				MovieTable.insert { // add some not existing file
					it[MovieTable.name] = "dummy"
					it[MovieTable.path] = "dummy-file-that-doesn't-exist.mp4"
				}
				MovieTable.selectAll().count() shouldBe 1

				DatabaseHelper.clean(sqlite.db)

				MovieTable.selectAll().count() shouldBe 0
			}
		}
	}
})