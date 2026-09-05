import java.util.Properties
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import javax.inject.Inject
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.gradle.process.ExecOperations
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
// 버전 (release 변형에만 적용, debug는 고정값이라 커밋마다 구성 캐시가 깨지지 않는다)
//   versionCode = origin/main 커밋 수(스쿼시 머지 1 PR = +1) — -PversionCode=N 으로 덮어쓰기.
//   git이 없거나 값이 숫자가 아니면 조용히 1로 떨어지지 않고 빌드를 실패시킨다.
//   Play 업로드 시에는 GPP resolutionStrategy=AUTO가 스토어 최대값+1로 다시 맞춘다.
// ---------------------------------------------------------------------------
// 구성 캐시 규칙: 아래 Provider 람다들은 스크립트 멤버를 캡처하면 안 된다(리터럴만 사용). 버전 접두 "1.0"은 두 곳에 리터럴로 적는다.
abstract class MainCommitCountSource : ValueSource<Int, MainCommitCountSource.Params> {
    interface Params : ValueSourceParameters { val override: Property<String> }
    @get:Inject abstract val execOperations: ExecOperations
    override fun obtain(): Int {
        val override = parameters.override.orNull
        if (override != null) {
            return override.trim().toIntOrNull() ?: throw GradleException("-PversionCode 는 정수여야 합니다: '$override'")
        }
        val out = ByteArrayOutputStream()
        execOperations.exec {
            commandLine("git", "rev-list", "--count", "origin/main")
            standardOutput = out
            isIgnoreExitValue = true
        }
        return out.toString().trim().toIntOrNull()
            ?: throw GradleException("versionCode 를 계산할 수 없습니다 (git rev-list --count origin/main 실패). -PversionCode=N 으로 지정하세요.")
    }
}
val releaseVersionCode: Provider<Int> = providers.of(MainCommitCountSource::class) {
    parameters.override.set(providers.gradleProperty("versionCode"))
}
val releaseVersionName: Provider<String> = releaseVersionCode.map { "1.0.$it" }

// ---------------------------------------------------------------------------
// 릴리스 서명: 루트 keystore.properties(gitignore) 또는 환경변수 POH_*. 네 값이 모두 있어야 서명한다.
// 일부만 있으면 어떤 값이 빠졌는지 알려주며 실패한다. 전부 없으면 미서명 빌드(R8 검증용)이고 publish 태스크는 실패한다.
// 생성 절차는 docs/RELEASE.md 참조.
// ---------------------------------------------------------------------------
val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) FileInputStream(f).use { load(it) }
}
fun signing(key: String, env: String): String? =
    keystoreProps.getProperty(key)?.takeIf { it.isNotBlank() } ?: System.getenv(env)?.takeIf { it.isNotBlank() }
val signingValues = mapOf(
    "storeFile" to signing("storeFile", "POH_STORE_FILE"),
    "storePassword" to signing("storePassword", "POH_STORE_PASSWORD"),
    "keyAlias" to signing("keyAlias", "POH_KEY_ALIAS"),
    "keyPassword" to signing("keyPassword", "POH_KEY_PASSWORD")
)
val missingSigning = signingValues.filterValues { it == null }.keys
val hasReleaseSigning = missingSigning.isEmpty()
if (missingSigning.size in 1..3) {
    throw GradleException("릴리스 서명 설정이 불완전합니다. 빠진 값: $missingSigning (keystore.properties 또는 POH_* 환경변수)")
}

android {
    // namespace(R 클래스·코드 패키지)는 그대로 두고 applicationId(스토어 식별자)만 브랜드로 바꿨다.
    namespace = "com.example.powerofhabit"
    compileSdk = 37
    defaultConfig {
        applicationId = "com.woodpeckerai.powerofhabit"
        minSdk = 24
        targetSdk = 37
        // debug 기본값. release는 아래 androidComponents.onVariants 에서 git 기반 값으로 덮어쓴다.
        versionCode = 1
        versionName = "1.0-dev"
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = rootProject.file(signingValues.getValue("storeFile")!!)
                storePassword = signingValues.getValue("storePassword")
                keyAlias = signingValues.getValue("keyAlias")
                keyPassword = signingValues.getValue("keyPassword")
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

androidComponents {
    onVariants(selector().withBuildType("release")) { variant ->
        variant.outputs.forEach { output ->
            output.versionCode.set(releaseVersionCode)
            output.versionName.set(releaseVersionName)
        }
    }
}

// 업로드(외부 효과)는 반드시 서명된 산출물이어야 한다 — 미서명이면 R8 빌드를 시작하기 전에 실패시킨다.
gradle.taskGraph.whenReady {
    val wantsPublish = allTasks.any { it.project == project && it.name.startsWith("publish") }
    val requireSigning = wantsPublish || providers.gradleProperty("requireSigning").isPresent
    if (requireSigning && !hasReleaseSigning) {
        throw GradleException("릴리스 서명이 설정되지 않았습니다. keystore.properties 또는 POH_* 환경변수를 준비하세요 (docs/RELEASE.md §0-1).")
    }
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
    // 스토어에 이미 있는 최대 versionCode + 1 로 맞춰 "already used" 거절을 막는다.
    resolutionStrategy.set(com.github.triplet.gradle.androidpublisher.ResolutionStrategy.AUTO)
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

  // JSON 내보내기/가져오기 (data/transfer)
  implementation(libs.kotlinx.serialization.json)

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
    val localPropertiesFile = rootProject.file("local.properties")
    val releaseLabel = releaseVersionName
    doLast {
        val versionLabel = runCatching { releaseLabel.get() }.getOrDefault("1.0-dev")
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
                    val targetName = "power-of-habit-v${versionLabel}_${timestamp}-debug.apk"
                    apkFile.copyTo(File(destDir, targetName), overwrite = true)
                    println("APK copied to Google Drive: ${destDir.absolutePath}/$targetName")
                }
                // 서명 여부에 따라 이름이 다르다: app-release.apk(서명) / app-release-unsigned.apk(미서명)
                val releaseApk = listOf("app-release.apk", "app-release-unsigned.apk")
                    .map { buildDir.file("outputs/apk/release/$it").get().asFile }
                    .firstOrNull { it.exists() }
                if (releaseApk != null) {
                    val suffix = if (releaseApk.name.contains("unsigned")) "release-unsigned" else "release"
                    val targetName = "power-of-habit-v${versionLabel}_${timestamp}-$suffix.apk"
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
