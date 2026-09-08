import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.google.services)
}

// local.properties 에서 API 키 로드
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
fun localProperty(key: String): String = localProperties.getProperty(key).orEmpty()

// 서버 주소는 빌드 타입별로 나눠 주입한다.
private val DefaultReleaseBaseUrl = "https://api.moamap.co.kr/"
private val DefaultDebugBaseUrl = "https://api-dev.moamap.co.kr/"

fun baseUrlOf(key: String, fallback: String): String = localProperty(key).ifEmpty { fallback }

android {
    namespace = "com.moamap.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.moamap.app"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Mapbox public 토큰
        resValue("string", "mapbox_access_token", localProperty("MAPBOX_PUBLIC_TOKEN"))

        // Kakao 네이티브 앱 키
        buildConfigField("String", "KAKAO_NATIVE_APP_KEY", "\"${localProperty("KAKAO_NATIVE_APP_KEY")}\"")
        manifestPlaceholders["KAKAO_NATIVE_APP_KEY"] = localProperty("KAKAO_NATIVE_APP_KEY")

        // Kakao REST API 키. 장소 검색(로컬 API)에 쓴다.
        //
        // 이 키는 APK 에서 뽑아낼 수 있고 플랫폼 제한으로 막히지 않는다. 네이티브 앱 키와 달리
        // 패키지명·키 해시를 검증하지 않아서다. 서버가 검색을 대신하는 엔드포인트가 생기면
        // 앱에서 걷어낸다 - 검색은 PlaceSearchRepository 뒤에 있어 구현체만 바꾸면 된다.
        buildConfigField("String", "KAKAO_REST_API_KEY", "\"${localProperty("KAKAO_REST_API_KEY")}\"")
    }

    buildTypes {
        debug {
            buildConfigField("String", "BASE_URL", "\"${baseUrlOf("DEBUG_BASE_URL", DefaultDebugBaseUrl)}\"")
        }
        release {
            buildConfigField("String", "BASE_URL", "\"${baseUrlOf("RELEASE_BASE_URL", DefaultReleaseBaseUrl)}\"")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
        resValues = true
    }
    testOptions {
        unitTests {
            // 카카오 SDK 가 Activity 컨텍스트를 요구해 ViewModel 시그니처에 Context 가 남는다.
            // 유닛 테스트에서 형식적인 Context 인스턴스를 만들 수 있도록,
            // android.jar 스텁이 예외 대신 기본값을 돌려주게 한다.
            isReturnDefaultValues = true
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.coil.compose)
    // 이게 없으면 AsyncImage 가 content:// 만 그리고 http(s) 주소는 조용히 실패한다.
    implementation(libs.coil.network.okhttp)
    implementation(libs.haze)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // 카카오 SDK 전체 기능
    implementation(libs.kakao.sdk.all)

    // Mapbox Maps SDK v11 + Compose 확장
    implementation(libs.mapbox.maps)
    implementation(libs.mapbox.maps.compose)

    // Hilt DI
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Navigation Compose
    implementation(libs.androidx.navigation.compose)

    // Retrofit + OkHttp + kotlinx.serialization
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)

    // 토큰 저장 (DataStore Preferences)
    implementation(libs.androidx.datastore.preferences)
}
