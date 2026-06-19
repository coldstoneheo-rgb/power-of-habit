import java.util.Properties
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.hilt.android)
  alias(libs.plugins.ksp)
}

android {
    namespace = "com.example.powerofhabit"
    compileSdk = 37
    defaultConfig {
        applicationId = "com.example.powerofhabit"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
      compose = true
      aidl = false
      buildConfig = false
      shaders = false
    }

    packaging {
      resources {
        excludes += "/META-INF/{AL2.0,LGPL2.1}"
      }
    }

    testOptions {
        unitTests {
            isReturnDefaultValues = true
        }
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
  coreLibraryDesugaring(libs.desugar.jdk.libs)
  val composeBom = platform(libs.androidx.compose.bom)
  implementation(composeBom)
  androidTestImplementation(composeBom)

  // Core Android dependencies
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)

  // Arch Components
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)

  // Compose
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.material.icons.core)
  // Tooling
  debugImplementation(libs.androidx.compose.ui.tooling)
  // Instrumented tests
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.test.manifest)

  // Local tests: jUnit, coroutines, Android runner
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation("org.mockito:mockito-core:5.8.0")

  // Instrumented tests: jUnit rules and runners
  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.androidx.test.espresso.core)

  // Navigation
  implementation(libs.androidx.navigation3.ui)
  implementation(libs.androidx.navigation3.runtime)
  implementation(libs.androidx.lifecycle.viewmodel.navigation3)

  // Room
  implementation(libs.room.runtime)
  implementation(libs.room.ktx)
  ksp(libs.room.compiler)

  // Hilt
  implementation(libs.hilt.android)
  ksp(libs.hilt.compiler)
  implementation(libs.hilt.navigation.compose)

  // Google Play Services Auth & API client for Drive
  implementation("com.google.android.gms:play-services-auth:20.7.0")
  implementation("com.google.apis:google-api-services-drive:v3-rev20230822-2.0.0") {
      exclude(group = "org.apache.httpcomponents")
  }
  implementation("com.google.api-client:google-api-client-android:2.2.0") {
      exclude(group = "org.apache.httpcomponents")
  }
}

tasks.register("copyApkToGoogleDrive") {
    val buildDir = layout.buildDirectory
    val versionName = android.defaultConfig.versionName ?: "1.0"
    val versionCode = android.defaultConfig.versionCode ?: 1
    val localPropertiesFile = rootProject.file("local.properties")
    doLast {
        val localProperties = Properties()
        if (localPropertiesFile.exists()) {
            FileInputStream(localPropertiesFile).use { stream ->
                localProperties.load(stream)
            }
        }
        val customPath = localProperties.getProperty("google.drive.apk.dir") as? String
        val destDir = if (!customPath.isNullOrBlank()) {
            File(customPath)
        } else {
            val fallback = File("G:/내 드라이브/AI-outputs/Android Studio/powerofhabit/apk")
            if (fallback.exists()) {
                fallback
            } else {
                null
            }
        }
        if (destDir == null) {
            println("Google Drive APK directory is not configured in local.properties and fallback path does not exist. Skipping copy.")
            return@doLast
        }
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("Asia/Seoul")
        }.format(Date())
        try {
            if (destDir.exists() || destDir.mkdirs()) {
                val apkFile = buildDir.file("outputs/apk/debug/app-debug.apk").get().asFile
                if (apkFile.exists()) {
                    val targetName = "power-of-habit-v${versionName}_c${versionCode}_${timestamp}-debug.apk"
                    apkFile.copyTo(File(destDir, targetName), overwrite = true)
                    println("APK copied to Google Drive: ${destDir.absolutePath}/$targetName")
                }
                val releaseApk = buildDir.file("outputs/apk/release/app-release-unsigned.apk").get().asFile
                if (releaseApk.exists()) {
                    val targetName = "power-of-habit-v${versionName}_c${versionCode}_${timestamp}-release.apk"
                    releaseApk.copyTo(File(destDir, targetName), overwrite = true)
                    println("Release APK copied to Google Drive: ${destDir.absolutePath}/$targetName")
                }
            } else {
                println("Google Drive directory not accessible: ${destDir.absolutePath}")
            }
        } catch (e: Exception) {
            println("Failed to copy APK to Google Drive: ${e.message}")
        }
    }
}

tasks.matching { it.name == "assembleDebug" || it.name == "assembleRelease" }.all {
    finalizedBy("copyApkToGoogleDrive")
}
