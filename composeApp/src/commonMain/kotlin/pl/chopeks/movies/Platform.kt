package pl.chopeks.movies

import cafe.adriel.voyager.core.screen.Screen
import kotlinx.coroutines.CoroutineDispatcher


interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
expect fun getStartingScreen(): Screen
expect fun bestConcurrencyDispatcher(): CoroutineDispatcher