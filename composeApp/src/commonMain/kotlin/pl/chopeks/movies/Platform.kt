package pl.chopeks.movies

import cafe.adriel.voyager.core.screen.Screen


interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
expect fun getStartingScreen(): Screen
