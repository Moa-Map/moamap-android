package com.moamap.app.feature.mapdetail.data.repository

import com.moamap.app.feature.collection.data.remote.MapMemberSummaryDto
import com.moamap.app.feature.mapdetail.domain.model.MapMember
import com.moamap.app.feature.mapdetail.domain.model.MapRole

/** 탈퇴했거나 프로필을 못 읽은 사람. */
internal const val UNKNOWN_MEMBER = "알 수 없는 사용자"

fun MapMemberSummaryDto.toMapMember(): MapMember = MapMember(
    id = userId,
    name = nickname?.trim()?.takeIf { it.isNotBlank() } ?: UNKNOWN_MEMBER,
    imageUrl = profileImageUrl?.trim()?.takeIf { it.isNotBlank() },
    role = MapRole.from(role),
)
