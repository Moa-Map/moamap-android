package com.moamap.app.feature.mapdetail.domain.model

/**
 * 지도에 참여한 사람 한 명.
 *
 * [role] 은 서버 값을 그대로 옮긴다. 프라이빗 지도처럼 역할을 안 쓰는 화면이라도 도메인에서
 * 지우지 않는다 - 무엇을 감출지는 화면이 정한다.
 */
data class MapMember(
    val id: Long,
    val name: String,
    val imageUrl: String?,
    val role: MapRole,
    /** 등록해 승인된 장소 수. 서버가 세지 못했으면 null 이다 - 0 으로 채우면 틀린 값이 된다. */
    val placeCount: Long? = null,
)
