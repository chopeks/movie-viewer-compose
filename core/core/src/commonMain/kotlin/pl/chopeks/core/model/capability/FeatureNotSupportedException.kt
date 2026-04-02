package pl.chopeks.core.model.capability

typealias CapabilityGuard = (Capability) -> Boolean

class FeatureNotSupportedException(
	val capability: Capability
) : Exception("Feature not supported: $capability")


