package pl.chopeks.screenmodel.model

sealed class UiEffect {
	class Toast(val message: String) : UiEffect()
}