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

android {
    namespace = "com.example.moamap"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.example.moamap"
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

        // 디버그 게이트웨이. 로컬 백엔드를 보려면 local.properties 에 BASE_URL 을 넣어 덮어쓴다.
        // 예) BASE_URL=http://10.0.2.2:8083/
        val baseUrl = localProperty("BASE_URL").ifEmpty { "http://125.6.39.211/" }
        buildConfigField("String", "BASE_URL", "\"$baseUrl\"")
    }

    buildTypes {
        debug {
            // BASE_URL 은 defaultConfig 에서 주입한다.
        }
        release {
            // TODO: https 도메인 확보 후 local.properties 대신 서명 파이프라인에서 주입
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
    implementation(project(":core:walksession"))

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

    // Wearable Data Layer (워치 세션 수신)
    implementation(libs.play.services.wearable)
    implementation(libs.kotlinx.coroutines.play.services)
}
