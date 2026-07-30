package com.example.moamap.feature.mapdetail.domain.model

import androidx.compose.runtime.Immutable
import com.example.moamap.feature.collection.domain.model.MapType

/**
 * 지도 설명·상세 화면이 함께 쓰는 지도 한 건.
 *
 * `GET /api/v1/maps/{mapId}` 응답에 제작자 닉네임([ownerName])을 얹은 모양이다. 서버 상세
 * 응답은 `ownerId` 만 주기 때문에 이름은 `GET /api/v1/users/profiles` 로 따로 받아 채운다.
 */
@Immutable
data class MapDetail(
    val id: Long,
    val title: String,
    val description: String?,
    val imageUrl: String?,
    /** 제작자 닉네임. 조회가 실패하면 null 이고, 그때는 화면에서 그 줄을 숨긴다. */
    val ownerName: String?,
    val type: MapType,
    val role: MapRole,
    val tags: List<String>,
    val memberCount: Int,
    val placeCount: Int,
    val joined: Boolean,
    /**
     * 가입할 때 자동으로 생기는 "나만의 지도" 다.
     *
     * 서버가 PRIVATE 타입으로 내려주기 때문에 [type] 으로는 일반 프라이빗 지도와 구분되지
     * 않는다. 서버가 따로 주는 이 값으로만 가릴 수 있다.
     */
    val personal: Boolean,
)

/**
 * 지도명 아래 역할 배지.
 *
 * 프라이빗 지도는 역할이 없는 지도라 배지를 띄우지 않는다. 참여하지 않은 지도도 마찬가지다.
 */
val MapDetail.roleBadge: String?
    get() = when {
        type == MapType.Private -> null
        role == MapRole.Owner -> "방장"
        role == MapRole.Admin -> "관리자"
        role == MapRole.Member -> "멤버"
        else -> null
    }

/** 상세 화면 우측 위에 놓일 액션. */
enum class MapDetailAction {
    /** 참여하기. 아직 참여하지 않은 공개 지도. */
    Join,

    /** 나가기. */
    Leave,

    /** 나가기지만 누를 수 없다. 서버가 거절할 게 뻔한 경우다. */
    LeaveDisabled,

    /** 아무것도 띄우지 않는다. */
    None,
}

/**
 * 우측 위 액션을 정한다.
 *
 * 서버 제약 두 가지가 그대로 규칙이 된다.
 * - `POST /maps/{mapId}/join` 은 공개 지도 전용이다. 프라이빗은 초대 코드로만 합류한다.
 * - `DELETE /maps/{mapId}/members/me` 는 OWNER 의 탈퇴를 막는다. 소유권 이전 API 도 없다.
 *
 * 프라이빗 지도를 만든 사람이 혼자 남았을 때만 예외로 나갈 수 있다. 이때 나가기는 지도를
 * 없애는 것과 같아서 지도 삭제(`DELETE /maps/{mapId}`)로 대신한다 - [leavingDeletesMap] 참고.
 *
 * TODO: 소유권 이전이나 OWNER 탈퇴가 생기면 [MapDetailAction.LeaveDisabled] 분기를 없앤다.
 */
val MapDetail.topBarAction: MapDetailAction
    get() = when {
        // 나만의 지도는 나갈 대상이 아니다. 자동으로 생기는 내 지도라 나가 봐야 다시 생긴다.
        personal -> MapDetailAction.None
        !joined -> if (type == MapType.Private) MapDetailAction.None else MapDetailAction.Join
        role != MapRole.Owner -> MapDetailAction.Leave
        type == MapType.Private && memberCount <= 1 -> MapDetailAction.Leave
        else -> MapDetailAction.LeaveDisabled
    }

/**
 * 나가기가 지도 삭제로 처리되는 경우.
 *
 * 프라이빗 지도를 만든 사람이 혼자 남은 상황이다. 조건을 [topBarAction] 과 나눠 갖지 않고
 * 여기서 다 본다 - 지도 삭제는 되돌릴 수 없어서, 호출부가 순서를 지켰겠거니 하면 안 된다.
 *
 * 나만의 지도도 같은 모양(PRIVATE·OWNER·혼자)이라 [personal] 을 먼저 걸러낸다. 나가기를
 * 띄우지 않으니 여기까지 올 일은 없지만, 삭제 판단이 화면 상태를 믿고 있으면 안 된다.
 */
val MapDetail.leavingDeletesMap: Boolean
    get() = joined &&
        !personal &&
        type == MapType.Private &&
        role == MapRole.Owner &&
        memberCount <= 1
