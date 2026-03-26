package pl.chopeks.usecase.video

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import pl.chopeks.core.data.repository.IVideoRepository
import pl.chopeks.core.model.Actor
import pl.chopeks.core.model.Category
import pl.chopeks.core.model.Video
import pl.chopeks.core.model.VideoChips

class GetVideosUseCase(
	private val repository: IVideoRepository
) {
	class RequestScope(
		var page: Long = 0,
		var selectedActors: List<Actor> = emptyList(),
		var selectedCategories: List<Category> = emptyList(),
		var filterType: Int = 0,
		var count: Int = 15,
	)

	class Result(
		val videos: List<Video>,
		val count: Long,
		val pageSize: Int
	)

	suspend operator fun invoke(
		actorLookup: (Int) -> Actor?,
		categoryLookup: (Int) -> Category?,
		block: RequestScope.() -> Unit,
	): Result {
		val scope = RequestScope().apply(block)
		val data = repository.getVideos(scope.page, scope.selectedActors, scope.selectedCategories, scope.filterType, scope.count)
		return Result(coroutineScope {
			data.movies.map { video ->
				async {
					enrichVideo(video, actorLookup, categoryLookup)
				}
			}.awaitAll()
		}, data.count, scope.count)
	}

	internal suspend fun enrichVideo(
		video: Video,
		actorLookup: (Int) -> Actor?,
		categoryLookup: (Int) -> Category?
	): Video {
		val info = repository.getInfo(video)
		return video.copy(
			image = repository.getImage(video),
			chips = VideoChips(
				actors = info.actors.mapNotNull(actorLookup),
				categories = info.categories.mapNotNull(categoryLookup)
			)
		)
	}
}