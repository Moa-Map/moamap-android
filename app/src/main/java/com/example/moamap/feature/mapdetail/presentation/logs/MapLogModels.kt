package com.example.moamap.feature.mapdetail.presentation.logs

import androidx.compose.runtime.Immutable
import com.example.moamap.feature.collection.domain.model.MapType

/** 타임라인 점 색으로 구분되는 로그 종류. */
internal enum class MapLogType {
    PlaceAdded,
    PlaceRemoved,

    /** 권한 위임·회수. 권한 개념이 없는 프라이빗 지도에는 나오지 않는다. */
    RoleChanged,
}

/**
 * 활동 내역 한 건.
 *
 * 화면에 그릴 형태 그대로 담는다. 서버 연동이 붙으면 매퍼가 이 모양으로 바꿔 준다.
 */
@Immutable
internal data class MapLogUiModel(
    val id: Long,
    val type: MapLogType,
    val userName: String,
    val userImageUrl: String?,
    /**
     * 사용자명 옆 역할 태그.
     *
     * `MapRole` 이 아니라 문자열이다. 프라이빗 지도에는 역할이 없어서 아예 그리지 않는데,
     * enum 을 들고 있으면 "프라이빗인데 역할이 있는" 상태가 표현 가능해진다.
     */
    val roleTag: String?,
    val message: String,
    /** 장소 추가에 사진이 딸린 경우. 없으면 카드에 글만 들어간다. */
    val imageUrl: String?,
    /**
     * 이미 "2시간 전" 형태다.
     *
     * 서버는 ISO 시각을 주겠지만, 여기서 시각을 들고 있으면 포맷 코드가 화면으로 들어온다.
     * 계산은 연동할 때 매퍼가 맡는다.
     */
    val timeAgo: String,
)

/**
 * 일반 참여자가 올린 장소 등록 요청.
 *
 * 공개 지도의 방장·관리자에게만 보인다.
 */
@Immutable
internal data class PendingRequestUiModel(
    val id: Long,
    val userName: String,
    val userImageUrl: String?,
    val message: String,
    val timeAgo: String,
)

/**
 * 지도 타입에 맞지 않는 것을 걸러낸다.
 *
 * 프라이빗 지도는 권한 자체가 없다. 권한 변경 로그뿐 아니라 **사용자명 옆 역할 태그도** 나올
 * 수 없다 - 로그 종류만 거르면 남은 장소 추가·삭제 로그에 "방장" 이 그대로 붙는다.
 * 서버가 실수로 내려줘도 화면에 흘리지 않는다.
 */
internal fun List<MapLogUiModel>.forMapType(type: MapType): List<MapLogUiModel> = when (type) {
    MapType.Private ->
        filter { log -> log.type != MapLogType.RoleChanged }
            .map { log -> log.copy(roleTag = null) }

    MapType.Community -> this
}
