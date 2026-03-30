package pl.chopeks.movies.composables.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import pl.chopeks.movies.composables.state.AlertDialogState.Companion.Saver

class AlertDialogState(
	initialValue: Boolean = true,
	private val confirmValueChange: (Boolean) -> Boolean = { true },
) {
	private var _isVisible by mutableStateOf(initialValue)

	val currentValue: Boolean
		get() = _isVisible

	val isVisible: Boolean
		get() = _isVisible

	fun show() {
		if (confirmValueChange(true)) {
			_isVisible = true
		}
	}

	fun hide() {
		if (confirmValueChange(false)) {
			_isVisible = false
		}
	}


	companion object {
		fun Saver(
			confirmValueChange: (Boolean) -> Boolean
		): Saver<AlertDialogState, Boolean> = Saver(
			save = { it.currentValue },
			restore = { savedValue ->
				AlertDialogState(
					initialValue = savedValue,
					confirmValueChange = confirmValueChange,
				)
			},
		)
	}
}

@Composable
fun rememberAlertDialogState(
	initialValue: Boolean = false,
	confirmValueChange: (Boolean) -> Boolean = { true }
): AlertDialogState {
	return rememberSaveable(
		saver = Saver(confirmValueChange = confirmValueChange)
	) {
		AlertDialogState(
			initialValue = initialValue,
			confirmValueChange = confirmValueChange,
		)
	}
}