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
	private val videoRepository: IVideoRepository,
	private val duplicateRepository: IDuplicateRepository,
	private val dispatcher: CoroutineDispatcher = bestConcurrencyDispatcher()
) {
	suspend operator fun invoke(): List<Duplicates> = withContext(dispatcher) {
		duplicateRepository.getCertainDuplicates().map { duplicate ->
			async {
				val videos = duplicate.list
				duplicate.copy(
					list = listOf(
						videos.first().copy(image = videoRepository.getImage(videos.first())),
						videos.last().copy(image = videoRepository.getImage(videos.last()))
					)
				)
			}
		}.awaitAll()
	}
}