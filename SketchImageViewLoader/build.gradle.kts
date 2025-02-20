import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.internal.dsl.BaseAppModuleExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions

plugins {
    id("com.android.library")
    id("kotlin-android")
}

group = "com.github.mikaelzero"
setupLibraryModule {
    namespace = "net.mikaelzero.mojito.view.sketch"
    defaultConfig {
        minSdk = 16
    }
}

fun Project.setupLibraryModule(block: LibraryExtension.() -> Unit = {}) {
    setupBaseModule<LibraryExtension> {
        libraryVariants.all {
            generateBuildConfigProvider?.configure { enabled = false }
        }
        testOptions {
            unitTests.isIncludeAndroidResources = true
        }
        block()
    }
}

fun Project.setupAppModule(block: BaseAppModuleExtension.() -> Unit = {}) {
    setupBaseModule<BaseAppModuleExtension> {
        defaultConfig {
            versionCode = 1
            versionName = "1.0"
            vectorDrawables.useSupportLibrary = true
        }
        block()
    }
}

inline fun <reified T : BaseExtension> Project.setupBaseModule(crossinline block: T.() -> Unit = {}) {
    extensions.configure<BaseExtension>("android") {
        compileSdkVersion(34)
        defaultConfig {
            minSdk = 16
            targetSdk = 34
            testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }
        (this as T).block()
    }
}

fun BaseExtension.kotlinOptions(block: KotlinJvmOptions.() -> Unit) {
    (this as ExtensionAware).extensions.configure("kotlinOptions", block)
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.21")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("me.panpf:sketch-gif:2.7.1")
    implementation("androidx.exifinterface:exifinterface:1.3.7")
    implementation(project(":mojito"))
}

