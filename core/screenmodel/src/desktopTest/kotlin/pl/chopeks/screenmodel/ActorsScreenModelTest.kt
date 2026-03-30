package pl.chopeks.screenmodel

import app.cash.turbine.test
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import pl.chopeks.core.data.IImageConverter
import pl.chopeks.core.data.repository.IActorRepository
import pl.chopeks.core.data.repository.IDuplicateRepository
import pl.chopeks.core.model.Actor
import pl.chopeks.screenmodel.ActorsScreenModel.Intent
import kotlin.io.encoding.Base64

@OptIn(ExperimentalCoroutinesApi::class)
class ActorsScreenModelTest : StringSpec({
	val repository = mockk<IActorRepository>()
	val duplicatesRepository = mockk<IDuplicateRepository>()
	val imageConverter = mockk<IImageConverter>()

	lateinit var screenModel: ActorsScreenModel

	beforeSpec {
		Dispatchers.setMain(StandardTestDispatcher())
	}

	afterSpec {
		Dispatchers.resetMain()
	}

	beforeTest {
		screenModel = ActorsScreenModel(
			repository,
			duplicatesRepository,
			imageConverter,
			Dispatchers.Main
		)
	}

	"LoadActors intent should fetch and update state" {
		runTest {
			val actors = listOf(Actor(1, "John Doe"), Actor(2, "Jane Smith"))
			coEvery { repository.getActors() } returns actors
			coEvery { repository.getImage(any()) } returns null

			screenModel.state.test {
				awaitItem().actors.shouldBeEmpty()

				screenModel.handleIntent(Intent.LoadActors)

				advanceUntilIdle()

				val state = awaitItem()

				state.isLoading shouldBe false
				state.actors shouldHaveSize 2
				state.actors[0].name shouldBe "Jane Smith"
				state.actors[1].name shouldBe "John Doe"
			}
		}
	}

	"UpdateSearch intent should filter actors" {
		runTest {
			val actors = listOf(Actor(1, "John Doe"), Actor(2, "Jane Smith"))
			coEvery { repository.getActors() } returns actors
			coEvery { repository.getImage(any()) } returns null

			screenModel.state.test {
				awaitItem().actors.shouldBeEmpty()

				screenModel.handleIntent(Intent.LoadActors)
				screenModel.handleIntent(Intent.UpdateSearch("Jane"))

				advanceUntilIdle()

				val state = expectMostRecentItem()
				state.searchFilter shouldBe "Jane"
				state.actors shouldHaveSize 1
				state.actors[0].name shouldBe "Jane Smith"
			}
		}
	}

	"AddActor intent should call repository and reload" {
		runTest {
			coEvery { repository.add(any(), any()) } returns Unit
			coEvery { repository.getActors() } returns emptyList()

			screenModel.handleIntent(Intent.AddActor("New Actor", null))
			advanceUntilIdle()

			coVerify { repository.add("New Actor", null) }
			coVerify { repository.getActors() }
		}
	}

	"EditActor intent should call repository and reload" {
		runTest {
			val actor = Actor(1, "Old Name")
			coEvery { repository.edit(any(), any(), any()) } returns Unit
			coEvery { repository.getActors() } returns emptyList()

			screenModel.handleIntent(Intent.EditActor(actor, "New Name", null))
			advanceUntilIdle()

			coVerify { repository.edit(1, "New Name", null) }
			coVerify { repository.getActors() }
		}
	}

	"RemoveActor intent should call repository and reload" {
		runTest {
			val actor = Actor(1, "John Doe")
			coEvery { repository.delete(any()) } returns Unit
			coEvery { repository.getActors() } returns emptyList()

			screenModel.handleIntent(Intent.RemoveActor(actor))
			advanceUntilIdle()

			coVerify { repository.delete(actor) }
			coVerify { repository.getActors() }
		}
	}

	"Deduplicate intent should call duplicatesRepository" {
		runTest {
			val actor = Actor(1, "John Doe")
			coEvery { duplicatesRepository.deduplicate(any()) } returns Unit

			screenModel.handleIntent(Intent.Deduplicate(actor))
			advanceUntilIdle()

			coVerify { duplicatesRepository.deduplicate(actor) }
		}
	}

	"LoadActors should background fetch images and update state" {
		runTest {
			val actor = Actor(1, "John Doe")
			val imageBase64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8/5+hHgAHggJ/PchI7wAAAABJRU5ErkJggg=="
			coEvery { repository.getActors() } returns listOf(actor)
			coEvery { repository.getImage(actor) } returns imageBase64

			screenModel.state.test {
				awaitItem().actors.shouldBeEmpty()

				screenModel.handleIntent(Intent.LoadActors)

				val state = awaitItem()
				state.actors shouldHaveSize 1
				state.actors[0].image shouldBe imageBase64
				state.actors[0].imageBytes shouldBe Base64.Mime.decode(imageBase64)
			}
		}
	}
})
