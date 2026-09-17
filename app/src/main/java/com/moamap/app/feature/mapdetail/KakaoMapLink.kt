package com.moamap.app.feature.mapdetail

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import java.net.URLEncoder

private const val TAG = "KakaoMapLink"

/** 카카오맵 앱의 장소 화면. 앱이 없으면 열 곳이 없어 [kakaoMapWebUrl] 로 넘어간다. */
internal fun kakaoMapAppUrl(kakaoPlaceId: String): String = "kakaomap://place?id=$kakaoPlaceId"

/** 카카오맵 웹의 장소 화면. 브라우저에서 열리고, 기기에 따라 카카오맵 앱으로 이어진다. */
internal fun kakaoMapWebUrl(kakaoPlaceId: String): String = "https://place.map.kakao.com/$kakaoPlaceId"

/**
 * 장소 id 가 없을 때 이름으로 찾는 카카오맵 웹 주소.
 *
 * id 는 서버 필수값이라 거의 늘 있지만, 비어 있다고 버튼이 아무 일도 하지 않으면 고장처럼 보인다.
 * 경로 조각이라 공백을 `+` 가 아니라 `%20` 으로 바꾼다. `Uri.encode` 는 유닛 테스트에서 쓸 수 없다.
 */
internal fun kakaoMapSearchUrl(placeName: String): String =
    "https://map.kakao.com/link/search/" +
        URLEncoder.encode(placeName.trim(), Charsets.UTF_8.name()).replace("+", "%20")

/**
 * 카카오맵에서 장소를 연다. 앱이 있으면 앱, 없으면 웹이다.
 *
 * 앱 설치 여부를 미리 묻지 않고 열어 본 뒤 실패하면 웹으로 넘어간다. 미리 물으려면 Android 11
 * 부터 매니페스트에 `<queries>` 를 적어야 하는데, 열어 보는 쪽은 그게 필요 없다.
 */
internal fun openKakaoMap(context: Context, kakaoPlaceId: String, placeName: String) {
    val id = kakaoPlaceId.trim()
    if (id.isNotEmpty() && startView(context, kakaoMapAppUrl(id))) return

    val webUrl = if (id.isNotEmpty()) kakaoMapWebUrl(id) else kakaoMapSearchUrl(placeName)
    if (!startView(context, webUrl)) {
        Log.w(TAG, "카카오맵을 열 앱이 없습니다")
    }
}

private fun startView(context: Context, url: String): Boolean = try {
    context.startActivity(
        Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
    true
} catch (e: ActivityNotFoundException) {
    false
}
