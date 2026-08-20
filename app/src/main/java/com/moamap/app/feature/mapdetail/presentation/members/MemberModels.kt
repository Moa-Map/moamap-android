package com.moamap.app.feature.mapdetail.presentation.members

import androidx.compose.runtime.Immutable
import com.moamap.app.feature.mapdetail.domain.model.MapMember
import com.moamap.app.feature.mapdetail.domain.model.MapRole

/** 멤버 카드에 붙는 역할. 프라이빗 지도에서는 그리지 않는다. */
internal enum class MemberRole(val label: String) {
    Owner("방장"),
    Admin("관리자"),
    Member("일반"),
}

/** 멤버 관리 목록의 한 사람. */
@Immutable
internal data class MemberUiModel(
    val id: Long,
    val name: String,
    val imageUrl: String?,
    val role: MemberRole,
)

/**
 * 역할 없는([MapRole.None]) 사람은 일반으로 본다.
 *
 * 목록에 실렸다는 건 그 지도의 멤버라는 뜻이다. 역할 자리가 비어 오더라도 카드를 빼거나 빈
 * 배지를 그리지 않는다.
 */
internal fun MapMember.toUiModel(): MemberUiModel = MemberUiModel(
    id = id,
    name = name,
    imageUrl = imageUrl,
    role = when (role) {
        MapRole.Owner -> MemberRole.Owner
        MapRole.Admin -> MemberRole.Admin
        MapRole.Member, MapRole.None -> MemberRole.Member
    },
)

/**
 * 이 사람에게 권한을 줄 수 있는가.
 *
 * 방장·관리자는 이미 권한이 있어 버튼이 붙지 않는다.
 */
internal fun MemberUiModel.canGrantRole(grantEnabled: Boolean): Boolean =
    grantEnabled && role == MemberRole.Member
