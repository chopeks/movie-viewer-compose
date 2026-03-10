package pl.chopeks.core.model

import kotlinx.serialization.Serializable

@Serializable
data class Duplicates(
	val list: List<Video>,
	val timestamp: Int = 0,
	val otherTimestamp: Int = 0
)

@Serializable
data class DuplicatesCount(
	val count: Int
)