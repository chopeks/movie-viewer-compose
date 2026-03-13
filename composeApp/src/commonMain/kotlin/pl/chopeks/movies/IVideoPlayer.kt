package pl.chopeks.movies

import kotlinx.rpc.annotations.Rpc
import pl.chopeks.core.model.Video

@Rpc
interface IVideoPlayer {
	suspend fun play(video: Video)
}