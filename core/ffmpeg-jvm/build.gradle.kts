plugins {
	alias(libs.plugins.mv.desktop.library)
}

kotlin {
	sourceSets {
		desktopMain.dependencies {
			api(projects.core.core)
			api(libs.kodein.di)
			api(libs.kotlinx.coroutines.swing)

			implementation(libs.javacv.platform)
		}
	}
}

configurations.all {
	exclude(group = "org.bytedeco", module = "artoolkitplus")
	exclude(group = "org.bytedeco", module = "flycapture")
	exclude(group = "org.bytedeco", module = "leptonica")
	exclude(group = "org.bytedeco", module = "tesseract")
	exclude(group = "org.bytedeco", module = "libdc1394")
	exclude(group = "org.bytedeco", module = "libfreenect")
	exclude(group = "org.bytedeco", module = "libfreenect2")
	exclude(group = "org.bytedeco", module = "librealsense")
	exclude(group = "org.bytedeco", module = "librealsense2")
	exclude(group = "org.bytedeco", module = "videoinput")
	exclude(group = "org.bytedeco", module = "openblas")
	exclude(group = "org.bytedeco", module = "opencv")
}