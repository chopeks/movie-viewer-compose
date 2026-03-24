package pl.chopeks.core.data

import kotlinx.coroutines.CoroutineDispatcher

expect fun bestConcurrencyDispatcher(): CoroutineDispatcher
