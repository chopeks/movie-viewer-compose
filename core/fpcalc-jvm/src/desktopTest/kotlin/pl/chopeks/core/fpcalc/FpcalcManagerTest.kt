package pl.chopeks.core.fpcalc

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import pl.chopeks.core.ffmpeg.FfmpegManager
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException

@OptIn(ExperimentalUnsignedTypes::class)
class FpcalcManagerTest : FunSpec({
	val process = mockk<Process>(relaxed = true)

	val processFfmpeg = mockk<Process>(relaxed = true)
	every { processFfmpeg.inputStream } returns ByteArrayInputStream(byteArrayOf())

	val ffmpegManager = mockk<FfmpegManager>()
	every { ffmpegManager.getFingerprintStream(any(), any(), any()) } returns processFfmpeg
	every { ffmpegManager.getAudioDuration(any()) } returns 123.5

	val fpcalcManager = FpcalcManager(ffmpegManager, processFactory = { _, _ -> process })

	test("isFfmpegAvailable returns true when exit code is 0") {
		every { process.waitFor() } returns 0
		fpcalcManager.isFpcalcAvailable() shouldBe true
	}

	test("isFfmpegAvailable returns false when command fails with IOException") {
		val failingManager = FpcalcManager(ffmpegManager, processFactory = { _, _ -> throw IOException() })
		failingManager.isFpcalcAvailable() shouldBe false
	}

	test("isFfmpegAvailable returns false when command fails with return code != 0") {
		every { process.waitFor() } returns 9009
		fpcalcManager.isFpcalcAvailable() shouldBe false
	}

	test("parseFingerprint returns correct fingerprint") {
		fpcalcManager.parseFingerprint("FINGERPRINT=1,2,3,4,5,42") shouldBe
			uintArrayOf(1u, 2u, 3u, 4u, 5u, 42u)
	}

	test("getFingerprint returns correct fingerprint") {
		val expectedOutput = "FINGERPRINT=1,2,3,4,5,42"

		every { process.inputStream } returns ByteArrayInputStream(expectedOutput.toByteArray())
		every { process.waitFor(any(), any()) } returns true

		fpcalcManager.getFingerprint(File("dummy.mp4")) shouldBe
			uintArrayOf(1u, 2u, 3u, 4u, 5u, 42u)
	}

	test("getFingerprint returns null in case if piped stream has no audio") {
		every { ffmpegManager.getAudioDuration(any()) } returns null
		every { process.inputStream } returns ByteArrayInputStream(byteArrayOf())
		every { process.waitFor(any(), any()) } returns true

		fpcalcManager.getFingerprint(File("dummy.mp4")) shouldBe null
	}
})
