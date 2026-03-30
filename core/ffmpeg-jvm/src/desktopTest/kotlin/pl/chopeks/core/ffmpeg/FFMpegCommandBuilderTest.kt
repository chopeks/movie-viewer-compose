package pl.chopeks.core.ffmpeg

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import pl.chopeks.core.ffmpeg.utils.formatDurationToFfmpegFormat
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class FFMpegCommandBuilderTest : StringSpec({
	"should conform to the ffmpeg format" {
		formatDurationToFfmpegFormat(0) shouldBe
			"00.000"
		formatDurationToFfmpegFormat(5.milliseconds.inWholeMilliseconds) shouldBe
			"00.005"
		formatDurationToFfmpegFormat(1.seconds.inWholeMilliseconds) shouldBe
			"01.000"
		formatDurationToFfmpegFormat(1.seconds.inWholeMilliseconds - 1) shouldBe
			"00.999"
		formatDurationToFfmpegFormat(1.minutes.inWholeMilliseconds) shouldBe
			"01:00.000"
		formatDurationToFfmpegFormat(1.minutes.inWholeMilliseconds - 1) shouldBe
			"59.999"
		formatDurationToFfmpegFormat(1.hours.inWholeMilliseconds) shouldBe
			"01:00:00.000"
		formatDurationToFfmpegFormat(1.hours.inWholeMilliseconds - 1) shouldBe
			"59:59.999"
		formatDurationToFfmpegFormat(-1.hours.inWholeMilliseconds) shouldBe
			"00.000"
	}
})