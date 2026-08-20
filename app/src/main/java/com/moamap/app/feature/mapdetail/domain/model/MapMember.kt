package com.moamap.app.feature.mapdetail.domain.model

/**
 * 지도에 참여한 사람 한 명.
 *
 * 등록한 장소 수는 들지 않는다. 서버 응답이 수정되면 그때 반영한다.
 *
 * [role] 은 서버 값을 그대로 옮긴다. 프라이빗 지도처럼 역할을 안 쓰는 화면이라도 도메인에서
 * 지우지 않는다 - 무엇을 감출지는 화면이 정한다.
 */
data class MapMember(
    val id: Long,
    val name: String,
    val imageUrl: String?,
    val role: MapRole,
)
