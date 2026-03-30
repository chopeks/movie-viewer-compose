package pl.chopeks.screenmodel

import app.cash.turbine.test
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import pl.chopeks.core.data.IImageConverter
import pl.chopeks.core.data.repository.ICategoryRepository
import pl.chopeks.core.model.Category
import pl.chopeks.screenmodel.CategoriesScreenModel.Intent
import kotlin.io.encoding.Base64

@OptIn(ExperimentalCoroutinesApi::class)
class CategoriesScreenModelTest : StringSpec({
	val repository = mockk<ICategoryRepository>()
	val imageConverter = mockk<IImageConverter>()
	val testDispatcher = StandardTestDispatcher()

	lateinit var screenModel: CategoriesScreenModel

	beforeSpec {
		Dispatchers.setMain(testDispatcher)
	}

	afterSpec {
		Dispatchers.resetMain()
	}

	beforeTest {
		screenModel = CategoriesScreenModel(
			repository,
			imageConverter,
			Dispatchers.Main
		)
	}

	"LoadCategories intent should fetch and update state" {
		runTest {
			val categories = listOf(Category(1, "Drama"), Category(2, "Comedy"))
			coEvery { repository.getCategories() } returns categories
			coEvery { repository.getImage(any()) } returns null

			screenModel.state.test {
				awaitItem() // Initial state
				screenModel.handleIntent(Intent.LoadCategories)

				// Skip loading state
				var state = awaitItem()
				while (state.isLoading) {
					state = awaitItem()
				}
				
				state.isLoading shouldBe false
				state.categories shouldHaveSize 2
				state.categories[0].name shouldBe "Comedy"
				state.categories[1].name shouldBe "Drama"
			}
		}
	}

	"UpdateSearch intent should filter categories" {
		runTest {
			val categories = listOf(Category(1, "Drama"), Category(2, "Comedy"))
			coEvery { repository.getCategories() } returns categories
			coEvery { repository.getImage(any()) } returns null

			screenModel.state.test {
				awaitItem() // Initial state
				screenModel.handleIntent(Intent.LoadCategories)
				
				// Skip to loaded state
				var state = awaitItem()
				while (state.isLoading) {
					state = awaitItem()
				}

				screenModel.handleIntent(Intent.UpdateSearch("Dram"))
				
				// Skip to filtered state
				state = awaitItem()
				while (state.searchFilter != "Dram") {
					state = awaitItem()
				}

				state.searchFilter shouldBe "Dram"
				state.categories shouldHaveSize 1
				state.categories[0].name shouldBe "Drama"
			}
		}
	}

	"AddCategory intent should call repository and reload" {
		runTest {
			coEvery { repository.add(any(), any()) } returns Unit
			coEvery { repository.getCategories() } returns emptyList()
			coEvery { repository.getImage(any()) } returns null

			screenModel.handleIntent(Intent.AddCategory("New Cat", null))
			advanceUntilIdle()

			coVerify { repository.add("New Cat", null) }
			coVerify { repository.getCategories() }
		}
	}

	"EditCategory intent should call repository and reload" {
		runTest {
			val category = Category(1, "Old Name")
			coEvery { repository.edit(any(), any(), any()) } returns Unit
			coEvery { repository.getCategories() } returns emptyList()
			coEvery { repository.getImage(any()) } returns null

			screenModel.handleIntent(Intent.EditCategory(category, "New Name", null))
			advanceUntilIdle()

			coVerify { repository.edit(1, "New Name", null) }
			coVerify { repository.getCategories() }
		}
	}

	"RemoveCategory intent should call repository and reload" {
		runTest {
			val category = Category(1, "Drama")
			coEvery { repository.delete(category) } returns Unit
			coEvery { repository.getCategories() } returns emptyList()
			coEvery { repository.getImage(any()) } returns null

			screenModel.handleIntent(Intent.RemoveCategory(category))
			advanceUntilIdle()

			coVerify { repository.delete(category) }
			coVerify { repository.getCategories() }
		}
	}

	"LoadCategories should background fetch images and update state" {
		runTest {
			val category = Category(1, "Drama")
			val imageBase64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8/5+hHgAHggJ/PchI7wAAAABJRU5ErkJggg=="
			coEvery { repository.getCategories() } returns listOf(category)
			coEvery { repository.getImage(category) } returns imageBase64

			screenModel.state.test {
				awaitItem() // Initial state
				screenModel.handleIntent(Intent.LoadCategories)

				// Consume loading and loaded (no image) states
				var state = awaitItem()
				while (state.isLoading || state.categories.isEmpty() || state.categories.first().image != null) {
					if (state.categories.isNotEmpty() && state.categories.first().image == null) break
					state = awaitItem()
				}
				state.categories.first().image shouldBe null

				// Wait for image to fetch
				state = awaitItem()
				while (state.categories.first().image == null) {
					state = awaitItem()
				}
				
				state.categories.first().image shouldBe imageBase64
				state.categories.first().imageBytes shouldBe Base64.Mime.decode(imageBase64)
			}
		}
	}
})
