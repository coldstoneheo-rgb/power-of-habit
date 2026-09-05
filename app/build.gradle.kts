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
  alias(libs.plugins.play.publisher)
}

// ---------------------------------------------------------------------------
// 버전: versionCode는 main의 커밋 수(단조 증가)이며 -PversionCode=N 으로 덮어쓸 수 있다.
// 배포마다 손으로 올리지 않아도 Play가 요구하는 "항상 증가"를 만족한다.
// ---------------------------------------------------------------------------
val baseVersionName = "1.0"
val computedVersionCode: Int = (findProperty("versionCode") as String?)?.toIntOrNull()
    ?: runCatching {
        providers.exec { commandLine("git", "rev-list", "--count", "HEAD") }
            .standardOutput.asText.get().trim().toInt()
    }.getOrDefault(1)

// ---------------------------------------------------------------------------
// 릴리스 서명: 루트의 keystore.properties(gitignore) 또는 환경변수. 없으면 서명 없이 빌드된다(내부 테스트 업로드는 불가).
// 생성 절차는 docs/RELEASE.md 참조.
// ---------------------------------------------------------------------------
val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) FileInputStream(f).use { load(it) }
}
fun signing(key: String, env: String): String? = keystoreProps.getProperty(key)?.takeIf { it.isNotBlank() } ?: System.getenv(env)
val releaseStoreFile = signing("storeFile", "POH_STORE_FILE")
val releaseStorePassword = signing("storePassword", "POH_STORE_PASSWORD")
val releaseKeyAlias = signing("keyAlias", "POH_KEY_ALIAS")
val releaseKeyPassword = signing("keyPassword", "POH_KEY_PASSWORD")
val hasReleaseSigning = listOf(releaseStoreFile, releaseStorePassword, releaseKeyAlias, releaseKeyPassword).all { it != null }

android {
    // namespace(R 클래스·코드 패키지)는 그대로 두고 applicationId(스토어 식별자)만 브랜드로 바꿨다.
    namespace = "com.example.powerofhabit"
    compileSdk = 37
    defaultConfig {
        applicationId = "com.woodpeckerai.powerofhabit"
        minSdk = 24
        targetSdk = 37
        versionCode = computedVersionCode
        versionName = "$baseVersionName.$computedVersionCode"
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = rootProject.file(releaseStoreFile!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (hasReleaseSigning) signingConfig = signingConfigs.getByName("release")
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

// ---------------------------------------------------------------------------
// Play 내부 테스트 업로드 (Gradle Play Publisher). 서비스 계정 JSON은 루트 play-service-account.json(gitignore).
//   ./gradlew.bat publishReleaseBundle   → 내부 테스트 트랙에 AAB 업로드
// 파일이 없으면 publish 태스크만 실패하고 일반 빌드는 영향 없다.
// ---------------------------------------------------------------------------
play {
    val credentials = rootProject.file("play-service-account.json")
    if (credentials.exists()) serviceAccountCredentials.set(credentials)
    track.set("internal")
    defaultToAppBundles.set(true)
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

  // Home screen widgets (Glance)
  implementation(libs.androidx.glance.appwidget)

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

// ---------------------------------------------------------------------------
// (옵트인) 빌드 산출물을 Google Drive 동기화 폴더로 복사. local.properties에
//   google.drive.apk.dir=<경로>
// 가 있을 때만 동작한다. 실기기 확인은 adb install 또는 Play 내부 테스트를 우선한다.
// ---------------------------------------------------------------------------
tasks.register("copyApkToGoogleDrive") {
    val buildDir = layout.buildDirectory
    val versionName = android.defaultConfig.versionName ?: baseVersionName
    val versionCode = android.defaultConfig.versionCode ?: computedVersionCode
    val localPropertiesFile = rootProject.file("local.properties")
    doLast {
        val localProperties = Properties()
        if (localPropertiesFile.exists()) {
            FileInputStream(localPropertiesFile).use { stream ->
                localProperties.load(stream)
            }
        }
        val customPath = localProperties.getProperty("google.drive.apk.dir") as? String
        if (customPath.isNullOrBlank()) {
            return@doLast // 옵트인 아님 — 조용히 건너뜀
        }
        val destDir = File(customPath)
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
                val releaseApk = buildDir.file("outputs/apk/release/app-release.apk").get().asFile
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
