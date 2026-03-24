package pl.chopeks.screenmodel

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import pl.chopeks.core.data.bestConcurrencyDispatcher
import pl.chopeks.screenmodel.model.UiEffect

abstract class BaseScreenModel : ScreenModel {
	private val _effects = MutableSharedFlow<UiEffect>()
	val effects = _effects.asSharedFlow()

	protected fun launchSafe(
		dispatcher: CoroutineDispatcher = bestConcurrencyDispatcher(),
		onError: suspend (Throwable) -> Unit = ::emitEffect,
		block: suspend CoroutineScope.() -> Unit
	) = screenModelScope.launch(dispatcher) {
		try {
			block()
		} catch (e: Exception) {
			if (e is CancellationException)
				throw e
			onError(e)
		}
	}

	protected suspend fun emitEffect(effect: UiEffect) {
		_effects.emit(effect)
	}

	protected abstract suspend fun emitEffect(throwable: Throwable)
}