package pl.chopeks.core.utils

import pl.chopeks.core.model.capability.Capability
import pl.chopeks.core.model.capability.CapabilityGuard
import pl.chopeks.core.model.capability.FeatureNotSupportedException

@Throws(FeatureNotSupportedException::class)
context(guard: CapabilityGuard)
fun ensure(vararg caps: Capability) = caps.forEach { cap ->
	if (!guard(cap))
		throw FeatureNotSupportedException(cap)
}