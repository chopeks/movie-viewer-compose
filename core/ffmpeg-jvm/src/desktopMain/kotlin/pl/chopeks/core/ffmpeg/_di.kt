package pl.chopeks.core.ffmpeg

import org.kodein.di.DI
import org.kodein.di.bindProvider
import org.kodein.di.bindSingleton
import org.kodein.di.instance

val ffmpegModule = DI.Module("ffmpeg") {
	bindSingleton { FfmpegManager() }
	bindProvider { VideoComparator(instance()) }
}