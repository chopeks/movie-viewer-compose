package pl.chopeks.usecase

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CoroutineDispatcher
import pl.chopeks.core.data.repository.IDuplicateRepository
import pl.chopeks.core.data.repository.IVideoRepository
import pl.chopeks.core.model.Duplicates
import pl.chopeks.core.model.Video
import pl.chopeks.usecase.video.GetDuplicatesUseCase
import kotlin.coroutines.ContinuationInterceptor

class GetDuplicatesUseCaseTest : StringSpec({
	coroutineTestScope = true

	val videoRepo = mockk<IVideoRepository>()
	val duplicatesRepo = mockk<IDuplicateRepository>()

	"should fetch images for first and last video in parallel" {
		val testDispatcher = coroutineContext[ContinuationInterceptor.Key] as CoroutineDispatcher
		val path = "path/to/img"

		val useCase = GetDuplicatesUseCase(videoRepo, duplicatesRepo, testDispatcher)

		val v1 = Video(0, "x", 0)
		val v2 = Video(1, "y", 0)
		val v3 = Video(2, "z", 0)

		coEvery { duplicatesRepo.getCertainDuplicates() } returns listOf(Duplicates(list = listOf(v1, v2, v3)))
		coEvery { videoRepo.getImage(any()) } returns path

		val result = useCase()

		result.first().list.shouldHaveSize(2)

		with(result.first().list.first()) {
			id shouldBe 0
			image shouldBe path
		}

		with(result.first().list.last()) {
			id shouldBe 2
			image shouldBe path
		}

		coVerify(exactly = 1) { videoRepo.getImage(v1) }
		coVerify(exactly = 1) { videoRepo.getImage(v3) }
		coVerify(exactly = 0) { videoRepo.getImage(v2) }
	}
})