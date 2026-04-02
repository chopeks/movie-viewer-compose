package pl.chopeks.movies.tasks

import org.kodein.di.DI
import org.kodein.di.bindProvider
import org.kodein.di.bindSingleton
import org.kodein.di.instance
import pl.chopeks.core.data.ITaskManager
import pl.chopeks.core.ffmpeg.ffmpegModule
import pl.chopeks.core.fpcalc.fpcalcModule
import pl.chopeks.movies.tasks.duplicates.CollectFingerprintsUseCase
import pl.chopeks.movies.tasks.duplicates.CompareAudioUseCase
import pl.chopeks.movies.tasks.duplicates.CompareVideoFramesUseCase

val taskModule = DI.Module("taskModule") {
	import(fpcalcModule)
	import(ffmpegModule)

	bindSingleton<TaskManager> { TaskManager(instance(), instance(), instance()) }
	bindProvider<ITaskManager> { instance<TaskManager>() }

	bindProvider { CollectFingerprintsUseCase(instance(), instance()) }
	bindProvider { CompareAudioUseCase(instance(), instance()) }
	bindProvider { CompareVideoFramesUseCase(instance(), instance()) }

	bindProvider { DuplicatesSearchTask(instance(), instance(), instance(), instance()) }
	bindProvider { VideoLookupTask(instance(), instance(), instance(), instance()) }
}