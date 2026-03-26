package pl.chopeks.usecase

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import pl.chopeks.core.data.repository.IVideoRepository
import pl.chopeks.core.model.*
import pl.chopeks.usecase.video.GetVideosUseCase
import kotlin.system.measureTimeMillis

@OptIn(ExperimentalCoroutinesApi::class)
class GetVideosUseCaseTest : StringSpec({
	coroutineTestScope = true

	val repository = mockk<IVideoRepository>()
	coEvery { repository.getImage(any()) } returns "url"

	val useCase = GetVideosUseCase(repository)

	fun createVideo(id: Int) = Video(id = id, name = "Video $id", 0)
	val mockActor = Actor(id = 1, name = "Tom Hanks")
	val mockCategory = Category(id = 10, name = "Drama")

	"should fetch and enrich videos correctly" {
		val mockVideos = listOf(createVideo(1))
		val mockData = VideoContainer(movies = mockVideos, count = 1)
		val mockInfo = VideoInfo(actors = listOf(1), categories = listOf(10))

		coEvery {
			repository.getVideos(any(), any(), any(), any(), any())
		} returns mockData

		coEvery { repository.getInfo(any()) } returns mockInfo

		val result = useCase(
			actorLookup = { id -> if (id == 1) mockActor else null },
			categoryLookup = { id -> if (id == 10) mockCategory else null }
		) {
			page = 0
			count = 10
		}

		coVerify {
			repository.getVideos(
				from = 0,
				actors = emptyList(),
				categories = emptyList(),
				filter = 0,
				count = 10
			)
		}

		result.videos.size shouldBe 1
		result.videos.first().chips?.actors.shouldNotBeNull() shouldContain mockActor
		result.videos.first().chips?.categories.shouldNotBeNull() shouldContain mockCategory
		result.count shouldBe 1
		result.pageSize shouldBe 10
	}

	"should handle missing lookups gracefully (mapNotNull)" {
		val mockVideos = listOf(createVideo(1))
		coEvery { repository.getVideos(any(), any(), any(), any(), any()) } returns VideoContainer(mockVideos, 1)
		coEvery { repository.getInfo(any()) } returns VideoInfo(actors = listOf(-1), categories = listOf(-1))

		val result = useCase(
			actorLookup = { null },
			categoryLookup = { null }
		) { page = 0 }

		result.videos.first().chips?.actors.shouldBeEmpty()
		result.videos.first().chips?.categories.shouldBeEmpty()
	}

	"should process images in parallel" {
		val mockVideos = listOf(createVideo(1), createVideo(2), createVideo(3))
		coEvery { repository.getVideos(any(), any(), any(), any(), any()) } returns
			VideoContainer(mockVideos, 3)

		coEvery { repository.getInfo(any()) } coAnswers {
			delay(5000)
			VideoInfo(emptyList(), emptyList())
		}

		measureTimeMillis {
			useCase({ null }, { null }) {}
		} shouldBeLessThan 6000
	}
})