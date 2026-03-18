package pl.chopeks.movies.tasks

import com.chopeks.pl.chopeks.core.ffmpeg.ffmpegModule
import com.chopeks.pl.chopeks.core.fpcalc.fpcalcModule
import org.kodein.di.DI
import org.kodein.di.bindProvider
import org.kodein.di.bindSingleton
import org.kodein.di.instance
import pl.chopeks.movies.ITaskManager
import pl.chopeks.movies.tasks.duplicates.CollectFingerprintsUseCase
import pl.chopeks.movies.tasks.duplicates.CompareAudioUseCase

val taskModule = DI.Module("taskModule") {
	import(fpcalcModule)
	import(ffmpegModule)

	bindSingleton<TaskManager> { TaskManager(instance(), instance()) }
	bindProvider<ITaskManager> { instance<TaskManager>() }

	bindProvider { CollectFingerprintsUseCase(instance(), instance()) }
	bindProvider { CompareAudioUseCase(instance(), instance()) }

	bindProvider { DuplicatesSearchTask(instance(), instance()) }
	bindProvider { VideoLookupTask(instance(), instance(), instance(), instance()) }
}