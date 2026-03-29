package pl.chopeks.core.utils

inline fun <T> T.runIf(
	condition: Boolean,
	block: T.() -> T
): T = if (condition) block() else this

inline fun <T> T.runIf(
	predicate: (T) -> Boolean,
	block: T.() -> T
): T = runIf(predicate(this), block)