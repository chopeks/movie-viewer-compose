package pl.chopeks.usecase.video

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import pl.chopeks.core.data.bestConcurrencyDispatcher
import pl.chopeks.core.data.repository.IDuplicateRepository
import pl.chopeks.core.data.repository.IVideoRepository
import pl.chopeks.core.model.Duplicates

class GetDuplicatesUseCase(
	private val videoRepo: IVideoRepository,
	private val duplicatesRepo: IDuplicateRepository,
	private val dispatcher: CoroutineDispatcher = bestConcurrencyDispatcher()
) {
	suspend operator fun invoke(): List<Duplicates> = withContext(dispatcher) {
		duplicatesRepo.getCertainDuplicates().map { duplicate ->
			async {
				val videos = duplicate.list
				duplicate.copy(
					list = listOf(
						videos.first().copy(image = videoRepo.getImage(videos.first())),
						videos.last().copy(image = videoRepo.getImage(videos.last()))
					)
				)
			}
		}.awaitAll()
	}
}