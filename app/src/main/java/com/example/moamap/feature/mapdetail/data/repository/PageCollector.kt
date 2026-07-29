package com.example.moamap.feature.mapdetail.data.repository

import android.util.Log
import com.example.moamap.core.network.model.PageResponse

private const val TAG = "PageCollector"

/** 한 번에 받아 오는 장소 수. 서버 상한과 같은 값이라 왕복이 가장 적다. */
const val PLACE_PAGE_SIZE = 100

/** 페이지를 이어 받는 횟수의 상한. 서버가 `last` 를 잘못 내려도 무한히 돌지 않게 한다. */
const val MAX_PAGES = 20

/**
 * 마지막 페이지까지 이어 받아 하나로 합친다.
 *
 * 조회 API 에 영역(bbox) 파라미터가 없어 마커를 그리려면 전량이 필요하다. 덜 받으면 그
 * 장소는 지도를 움직여도 끝내 보이지 않는다.
 *
 * 멈추는 조건이 셋이다. `last` 가 참이거나, 빈 페이지가 오거나, [maxPages] 에 닿거나다.
 * 뒤의 둘은 서버 응답이 어긋났을 때의 안전장치다 - 한쪽만 믿으면 루프가 끝나지 않는다.
 */
suspend fun <T> collectAllPages(
    maxPages: Int = MAX_PAGES,
    fetchPage: suspend (page: Int) -> PageResponse<T>,
): List<T> {
    val all = mutableListOf<T>()

    for (page in 0 until maxPages) {
        val response = fetchPage(page)
        all += response.content

        if (response.last || response.content.isEmpty()) return all
    }

    Log.w(TAG, "페이지 상한 $maxPages 에 걸려 받은 데까지만 쓴다 (${all.size}건)")
    return all
}
