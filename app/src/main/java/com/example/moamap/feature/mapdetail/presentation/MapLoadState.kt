package com.example.moamap.feature.mapdetail.presentation

import com.example.moamap.core.network.ConnectionException
import com.example.moamap.feature.mapdetail.domain.model.MapDetail

/** 지도 한 건을 받아오는 동안의 상태. 설명 화면과 상세 화면이 함께 쓴다. */
sealed interface MapLoadState {
    data object Loading : MapLoadState
    data class Success(val map: MapDetail) : MapLoadState
    data class Error(val message: String) : MapLoadState
}

val MapLoadState.mapOrNull: MapDetail?
    get() = (this as? MapLoadState.Success)?.map

internal const val MAP_LOAD_FAILED_MESSAGE = "지도를 불러오지 못했어요"
internal const val JOIN_FAILED_MESSAGE = "지도에 참여하지 못했어요"
internal const val LEAVE_FAILED_MESSAGE = "지도에서 나가지 못했어요"
internal const val NETWORK_ERROR_MESSAGE = "네트워크에 연결할 수 없어요"

/**
 * 실패 안내 문구.
 *
 * 서버 메시지를 그대로 노출하지 않는다. `ApiException` 은 `[500] COMMON_005: ...` 처럼
 * 사용자에게 보여줄 수 없는 형태다. 연결 실패만 따로 가르고 나머지는 [fallback] 으로 묶는다.
 */
internal fun Throwable.toUserMessage(fallback: String): String =
    if (this is ConnectionException) NETWORK_ERROR_MESSAGE else fallback
