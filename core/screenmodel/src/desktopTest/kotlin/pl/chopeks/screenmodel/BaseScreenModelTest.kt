package pl.chopeks.screenmodel

import io.kotest.core.spec.style.StringSpec
import io.kotest.core.test.testCoroutineScheduler
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.timeout
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import pl.chopeks.screenmodel.model.UiEffect
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class BaseScreenModelTest : StringSpec({
	coroutineTestScope = true

	beforeSpec {
		Dispatchers.setMain(StandardTestDispatcher())
	}

	afterSpec {
		Dispatchers.resetMain()
	}

	"should return effect if exception is thrown in launchSafe" {
		val testDispatcher = coroutineContext[ContinuationInterceptor.Key] as CoroutineDispatcher

		val obj = object : BaseScreenModel() {
			fun run() = launchSafe(dispatcher = testDispatcher) {
				delay(500)
				throw RuntimeException("Exception")
			}

			override suspend fun emitEffect(throwable: Throwable) {
				emitEffect(UiEffect.Toast(throwable.message ?: "Test"))
			}
		}

		val emittedEffects = mutableListOf<UiEffect>()
		val job = launch {
			obj.effects.toList(emittedEffects)
		}

		obj.run()

		testCoroutineScheduler.advanceTimeBy(999)

		emittedEffects shouldHaveSize 1
		val effect = emittedEffects.first()

		effect.shouldBeInstanceOf<UiEffect.Toast>()
		effect.message shouldBe "Exception"

		job.cancel()
	}

	"should not emit effect if job is cancelled" {
		val obj = object : BaseScreenModel() {
			fun run() = launchSafe { throw CancellationException("User left screen") }
			override suspend fun emitEffect(throwable: Throwable) = emitEffect(UiEffect.Toast("Error"))
		}

		val emittedEffects = mutableListOf<UiEffect>()
		val job = launch(UnconfinedTestDispatcher(testCoroutineScheduler)) {
			obj.effects.timeout(1.seconds).toList(emittedEffects)
		}

		testCoroutineScheduler.advanceTimeBy(2000)

		obj.run()

		job.join()

		emittedEffects.shouldBeEmpty()

		job.cancel()
	}

	"should emit multiple effects for multiple errors" {
		val obj = object : BaseScreenModel() {
			fun run() = launchSafe { throw RuntimeException("Fail") }
			override suspend fun emitEffect(throwable: Throwable) = emitEffect(UiEffect.Toast("Fail"))
		}

		val emittedEffects = mutableListOf<UiEffect>()
		val job = launch(UnconfinedTestDispatcher(testCoroutineScheduler)) {
			obj.effects.timeout(1.seconds).toList(emittedEffects)
		}

		repeat(3) { obj.run() }

		testCoroutineScheduler.advanceTimeBy(2000)

		job.join()

		emittedEffects shouldHaveSize 3
		emittedEffects.forEach { effect ->
			effect.shouldBeInstanceOf<UiEffect.Toast>()
			effect.message shouldBe "Fail"
		}
		job.cancel()
	}
})