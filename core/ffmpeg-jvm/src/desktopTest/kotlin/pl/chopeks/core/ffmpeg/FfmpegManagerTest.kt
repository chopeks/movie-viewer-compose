package pl.chopeks.core.ffmpeg

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException

class FfmpegManagerTest : FunSpec({
    val process = mockk<Process>(relaxed = true)

    val ffmpegManager = FfmpegManager(processFactory = { _, _ -> process })

    test("isFfmpegAvailable returns true when exit code is 0") {
        every { process.waitFor() } returns 0
        ffmpegManager.isFfmpegAvailable() shouldBe true
    }

    test("isFfmpegAvailable returns false when command fails with IOException") {
        val failingManager = FfmpegManager(processFactory = { _, _ -> throw IOException() })
        failingManager.isFfmpegAvailable() shouldBe false
    }

    test("isFfmpegAvailable returns false when command fails with return code != 0") {
        every { process.waitFor() } returns 9009
        ffmpegManager.isFfmpegAvailable() shouldBe false
    }


    test("isFfprobeAvailable returns true when exit code is 0") {
        every { process.waitFor() } returns 0
        ffmpegManager.isFfprobeAvailable() shouldBe true
    }

    test("isFfprobeAvailable returns false when command fails with IOException") {
        val failingManager = FfmpegManager(processFactory = { _, _ -> throw IOException() })
        failingManager.isFfprobeAvailable() shouldBe false
    }

    test("isFfprobeAvailable returns false when command fails with return code != 0") {
        every { process.waitFor() } returns 9009
        ffmpegManager.isFfprobeAvailable() shouldBe false
    }


    test("getVideoDuration parses ffmpeg output into milliseconds") {
        val expectedOutput = "123.45"
        every { process.inputStream } returns ByteArrayInputStream(expectedOutput.toByteArray())
        every { process.waitFor(any(), any()) } returns true

        ffmpegManager.getVideoDuration(File("dummy.mp4")) shouldBe 123000L
    }

    test("getAudioDuration returns raw double duration") {
        val expectedOutput = "123.45"
        every { process.inputStream } returns ByteArrayInputStream(expectedOutput.toByteArray())
        every { process.waitFor() } returns 0

        ffmpegManager.getAudioDuration(File("dummy.mp4")) shouldBe 123.45
    }
})
