package com.moamap.app

import android.app.Application
import com.kakao.sdk.common.KakaoSdk
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MoaMapApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // 카카오 로그인 API 를 호출하기 전에 반드시 한 번 초기화해야 한다.
        // 네이티브 앱 키는 local.properties -> buildConfigField 로 주입된다.
        KakaoSdk.init(
            context = this,
            appKey = BuildConfig.KAKAO_NATIVE_APP_KEY,
            // 디버그 빌드에서만 SDK 내부 로그를 켠다. 로그인 실패 원인 추적에 필요하다.
            loggingEnabled = BuildConfig.DEBUG,
        )
    }
}
