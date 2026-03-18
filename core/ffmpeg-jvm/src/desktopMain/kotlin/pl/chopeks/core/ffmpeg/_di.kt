package com.chopeks.pl.chopeks.core.ffmpeg

import org.kodein.di.DI
import org.kodein.di.bindSingleton

val ffmpegModule = DI.Module("ffmpeg") {
	bindSingleton { FfmpegManager() }
}