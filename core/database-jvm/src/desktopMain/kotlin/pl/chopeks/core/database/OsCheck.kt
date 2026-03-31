package pl.chopeks.core.database

import java.util.*

internal object OsCheck {
	val operatingSystemType: OSType
		get() {
			val os = System.getProperty("os.name", "generic").lowercase(Locale.ENGLISH)
			return when {
				os.contains("mac") || os.contains("darwin") -> OSType.MacOS
				os.contains("win") -> OSType.Windows
				os.contains("nux") -> OSType.Linux
				else -> OSType.Other
			}
		}

	enum class OSType {
		Windows, MacOS, Linux, Other
	}
}