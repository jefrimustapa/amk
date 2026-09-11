import groovy.json.JsonSlurper

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// Execute prebuild.js to ensure fresh versioning info
tasks.register("runPrebuild") {
    doLast {
        val os = System.getProperty("os.name").lowercase()
        val isWindows = os.contains("win")
        val cmd = if (isWindows) listOf("cmd", "/c", "node scripts/prebuild.js") else listOf("node", "scripts/prebuild.js")
        ProcessBuilder(cmd)
            .directory(rootDir)
            .inheritIO()
            .start()
            .waitFor()
    }
}

fun loadBuildInfoMap(): Map<String, Any> {
    val buildInfoFile = file("$projectDir/build-info.json")
    if (buildInfoFile.exists()) {
        try {
            val parsed = JsonSlurper().parseText(buildInfoFile.readText())
            if (parsed is Map<*, *>) {
                @Suppress("UNCHECKED_CAST")
                return parsed as Map<String, Any>
            }
        } catch (e: Exception) {
            println("Failed to parse build-info.json: ${e.message}")
        }
    }
    return mapOf(
        "version" to "1.0.0",
        "versionName" to "1.0.0-dev.local",
        "versionCode" to 100000,
        "fullVersionString" to "1.0.0-dev.local.main",
        "channel" to "dev",
        "buildNumber" to "local",
        "gitCommit" to "head"
    )
}

val currentBuildInfo = loadBuildInfoMap()
val appVersionName = currentBuildInfo["versionName"]?.toString() ?: "1.0.0-dev"
val appVersionCode = (currentBuildInfo["versionCode"] as? Number)?.toInt() ?: 100000
val appVersionFull = currentBuildInfo["fullVersionString"]?.toString() ?: appVersionName
val appChannel = currentBuildInfo["channel"]?.toString() ?: "dev"
val appBuildNumber = currentBuildInfo["buildNumber"]?.toString() ?: "local"
val appGitCommit = currentBuildInfo["gitCommit"]?.toString() ?: "head"

android {
    namespace = "com.amk.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.amk.app"
        minSdk = 28 // Required for native BluetoothHidDevice
        targetSdk = 35
        versionCode = appVersionCode
        versionName = appVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "APP_VERSION_NAME", "\"$appVersionName\"")
        buildConfigField("String", "APP_VERSION_FULL", "\"$appVersionFull\"")
        buildConfigField("String", "APP_CHANNEL", "\"$appChannel\"")
        buildConfigField("String", "APP_BUILD_NUMBER", "\"$appBuildNumber\"")
        buildConfigField("String", "APP_GIT_COMMIT", "\"$appGitCommit\"")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
        }
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
