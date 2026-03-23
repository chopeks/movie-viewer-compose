package pl.chopeks.movies.composables.state

import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.ui.test.ExperimentalTestApi
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalTestApi::class)
class AlertDialogStateTest : StringSpec({
	"initial value should change isVisible" {
		AlertDialogState(initialValue = true).isVisible shouldBe true
		AlertDialogState(initialValue = false).isVisible shouldBe false
	}

	"show() should update isVisible to true" {
		runTest {
			val state = AlertDialogState(initialValue = false)
			state.show()
			state.isVisible shouldBe true
		}
	}

	"hide() should update isVisible to false" {
		runTest {
			val state = AlertDialogState(initialValue = true)
			state.hide()
			state.isVisible shouldBe false
		}
	}

	"confirmValueChange returning false should block show()" {
		runTest {
			val state = AlertDialogState(initialValue = false) { false }
			state.show()
			state.isVisible shouldBe false
		}
	}

	"confirmValueChange returning false should block hide()" {
		runTest {
			val state = AlertDialogState(initialValue = true) { false }
			state.hide()
			state.isVisible shouldBe true
		}
	}

	"Saver should correctly preserve and restore state" {
		val saver = AlertDialogState.Saver { true }
		val originalState = AlertDialogState(initialValue = true)

		val saved = with(saver) {
			SaverScope { true }.save(originalState)
		}

		saved shouldBe true

		val restored = saver.restore(saved!!)
		restored?.isVisible shouldBe true
	}

	"AlertDialogState should survive save and restore cycle" {
		val confirmChange: (Boolean) -> Boolean = { it }
		val originalState = AlertDialogState(initialValue = true, confirmValueChange = confirmChange)

		originalState.isVisible shouldBe true

		val saver = AlertDialogState.Saver(confirmChange)
		val savedValue = with(saver) {
			SaverScope { true }.save(originalState)
		}
		savedValue shouldBe true

		val restoredState = saver.restore(savedValue!!)

		restoredState.shouldNotBeNull()
		restoredState.isVisible shouldBe true

		runTest {
			restoredState.hide()
			restoredState.isVisible shouldBe true
		}
	}
})