package com.example.moamap.feature.mapdetail.presentation.members

import androidx.compose.runtime.Immutable

/** 멤버 카드에 붙는 역할. 프라이빗 지도에서는 그리지 않는다. */
internal enum class MemberRole(val label: String) {
    Owner("방장"),
    Admin("관리자"),
    Member("일반"),
}

/**
 * 멤버 관리 목록의 한 사람.
 *
 * 장소 수는 숫자로 들고 화면에서 `formatPlaceCount()` 로 그린다. 카드 메타를 그리는 다른
 * 화면들과 같은 방식이다.
 */
@Immutable
internal data class MemberUiModel(
    val id: Long,
    val name: String,
    val imageUrl: String?,
    val role: MemberRole,
    val placeCount: Int,
)

/**
 * 이 사람에게 권한을 줄 수 있는가.
 *
 * 방장·관리자는 이미 권한이 있어 버튼이 붙지 않는다. 시안에서도 일반 카드에만 버튼이 있다.
 */
internal fun MemberUiModel.canGrantRole(grantEnabled: Boolean): Boolean =
    grantEnabled && role == MemberRole.Member
