package pl.chopeks.core.ffmpeg

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.doubles.shouldBeLessThan
import io.mockk.mockk
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

class VideoComparatorTest : StringSpec({
	val comparator = VideoComparator(mockk())

	fun loadImage(name: String): BufferedImage {
		val stream = Thread.currentThread().contextClassLoader.getResourceAsStream(name)
			?: throw IllegalArgumentException("Could not find $name in src/test/resources")
		return ImageIO.read(stream)
	}

	"PSNR and SSIM should be perfect for identical images" {
		val img = loadImage("test.png")

		val psnr = comparator.calculatePSNR(img, img)
		val ssim = comparator.calculateSSIM(img, img)

		psnr shouldBeGreaterThan 99.0
		ssim shouldBeGreaterThan 0.9
	}

	"SSIM should decrease for different images" {
		val img1 = loadImage("test.png")
		val img2 = loadImage("test2.jpg")

		val ssim = comparator.calculateSSIM(img1, img2)

		// Typical "high similarity" is > 0.9, completely different is < 0.3
		ssim shouldBeLessThan 0.3
		ssim shouldBeGreaterThan -0.3
	}
})