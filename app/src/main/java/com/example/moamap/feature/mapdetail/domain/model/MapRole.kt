package com.example.moamap.feature.mapdetail.domain.model

/**
 * 지도 안에서의 내 역할. 서버 `myRole` 과 1:1 로 대응한다.
 *
 * 프라이빗 지도에도 값은 내려오지만(만든 사람이 [Owner]) 배지로는 쓰지 않는다.
 * 프라이빗은 역할 개념이 없는 지도다. 나가기 가능 여부를 가릴 때만 쓴다.
 */
enum class MapRole {
    Owner,
    Admin,
    Member,
    None,
    ;

    companion object {
        /** 모르는 값은 권한이 없는 쪽([None])으로 본다. */
        fun from(raw: String?): MapRole = when (raw) {
            "OWNER" -> Owner
            "ADMIN" -> Admin
            "MEMBER" -> Member
            else -> None
        }
    }
}
