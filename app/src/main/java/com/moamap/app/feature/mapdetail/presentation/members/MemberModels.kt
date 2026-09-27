package com.moamap.app.feature.mapdetail.presentation.members

import androidx.compose.runtime.Immutable
import com.moamap.app.feature.collection.domain.model.MapType
import com.moamap.app.feature.mapdetail.domain.model.MapMember
import com.moamap.app.feature.mapdetail.domain.model.MapRole

/** 멤버의 역할. 태그를 붙일지는 [MemberRoleDisplay] 가 정한다. */
internal enum class MemberRole {
    Owner,
    Admin,
    Member,
}

/** 멤버 관리 목록의 한 사람. */
@Immutable
internal data class MemberUiModel(
    val id: Long,
    val name: String,
    val imageUrl: String?,
    val role: MemberRole,
    /** 등록한 장소 수. 서버가 세지 못했으면 null 이고, 그때는 그 줄을 숨긴다. */
    val placeCount: Long? = null,
)

/**
 * 역할 없는([MapRole.None]) 사람은 일반으로 본다.
 *
 * 목록에 실렸다는 건 그 지도의 멤버라는 뜻이다. 역할 자리가 비어 오더라도 카드를 빼지 않는다.
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
    placeCount = placeCount,
)

/**
 * 이 사람에게 권한을 줄 수 있는가.
 *
 * 방장·관리자는 이미 권한이 있어 버튼이 붙지 않는다.
 */
internal fun MemberUiModel.canGrantRole(grantEnabled: Boolean): Boolean =
    grantEnabled && role == MemberRole.Member

/** 멤버 관리에서 역할을 어디까지 드러낼지. 지도 종류가 정한다. */
internal enum class MemberRoleDisplay {
    /** 커뮤니티: 방장·관리자 태그와 역할 안내. */
    All,

    /** 프라이빗: 만든 사람의 방장 태그만. 역할이 나뉘지 않아 관리자도 역할 안내도 없다. */
    OwnerOnly,

    /** 공식지도: 공공데이터를 받아 보는 지도라 역할이 뜻을 갖지 않는다. */
    None,
}

internal fun memberRoleDisplayOf(type: MapType): MemberRoleDisplay = when (type) {
    MapType.Community -> MemberRoleDisplay.All
    MapType.Private -> MemberRoleDisplay.OwnerOnly
    MapType.Official -> MemberRoleDisplay.None
}

/** 이름 옆에 붙일 태그. 일반 멤버에게는 붙지 않는다. */
internal fun MemberUiModel.tag(display: MemberRoleDisplay): MemberRole? = when {
    role == MemberRole.Owner && display != MemberRoleDisplay.None -> MemberRole.Owner
    role == MemberRole.Admin && display == MemberRoleDisplay.All -> MemberRole.Admin
    else -> null
}
