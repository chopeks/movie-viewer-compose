package pl.chopeks.core

import pl.chopeks.core.model.Video

interface IVideoPlayer: AutoCloseable {
	suspend fun play(video: Video)
}