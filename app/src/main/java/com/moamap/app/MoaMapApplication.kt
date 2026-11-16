package com.moamap.app

import android.app.Application
import com.kakao.sdk.common.KakaoSdk
import com.mapbox.bindgen.Value
import com.mapbox.common.MapboxCommonSettings
import com.mapbox.common.SettingsServiceFactory
import com.mapbox.common.SettingsServiceStorageType
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
        // 지도 글자는 기기 언어를 따라 영어로 나오기도 한다. 한국어로 받고, 한국어 이름이 없는 곳은 현지 이름이 나온다.
        // 표준 스타일은 앱에서 글자를 바꾸는 localizeLabels 를 막아 두어 Mapbox 공용 설정으로 정한다.
        // 지도 엔진은 PERSISTENT 쪽 설정을 본다(NON_PERSISTENT 로 넣으면 그대로 영어였다).
        SettingsServiceFactory.getInstance(SettingsServiceStorageType.PERSISTENT)
            .set(MapboxCommonSettings.LANGUAGE, Value.valueOf("ko"))
    }
}
