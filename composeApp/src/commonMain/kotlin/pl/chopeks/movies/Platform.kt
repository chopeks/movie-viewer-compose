package pl.chopeks.movies

import kotlinx.coroutines.CoroutineDispatcher


interface Platform {
    val name: String
}

expect fun getPlatform(): Platform

expect fun bestConcurrencyDispatcher(): CoroutineDispatcher