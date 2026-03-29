package pl.chopeks.screenmodel

import app.cash.turbine.test
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import pl.chopeks.screenmodel.model.UiEffect
import kotlin.coroutines.cancellation.CancellationException

@OptIn(ExperimentalCoroutinesApi::class)
class BaseScreenModelTest : StringSpec({
	beforeSpec {
		Dispatchers.setMain(StandardTestDispatcher())
	}

	afterSpec {
		Dispatchers.resetMain()
	}

	"should return effect if exception is thrown in launchSafe" {
		runTest {
			val obj = object : BaseScreenModel() {
				fun run() = launchSafe {
					delay(500)
					throw RuntimeException("Exception")
				}

				override suspend fun emitEffect(throwable: Throwable) {
					emitEffect(UiEffect.Toast(throwable.message ?: "Test"))
				}
			}

			obj.effects.test {
				obj.run()

				val effect = awaitItem()
				effect.shouldBeInstanceOf<UiEffect.Toast>()
				effect.message shouldBe "Exception"
			}
		}
	}

	"should not emit effect if job is cancelled" {
		runTest {
			val obj = object : BaseScreenModel() {
				fun run() = launchSafe { throw CancellationException("User left screen") }
				override suspend fun emitEffect(throwable: Throwable) = emitEffect(UiEffect.Toast("Error"))
			}

			obj.effects.test {
				obj.run()
				expectNoEvents()
			}
		}
	}

	"should emit multiple effects for multiple errors" {
		runTest {
			val obj = object : BaseScreenModel() {
				fun run() = launchSafe { throw RuntimeException("Fail") }
				override suspend fun emitEffect(throwable: Throwable) = emitEffect(UiEffect.Toast("Fail"))
			}

			obj.effects.test {
				repeat(3) {
					obj.run()
					val effect = awaitItem()
					effect.shouldBeInstanceOf<UiEffect.Toast>()
					effect.message shouldBe "Fail"
				}
			}
		}
	}
})
