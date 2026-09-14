package com.moamap.app.feature.mapdetail.data.repository

import com.moamap.app.feature.explore.data.remote.PendingPlaceDto
import com.moamap.app.feature.mapdetail.domain.model.PendingPlace

/**
 * 승인 대기 응답을 도메인으로 옮긴다.
 *
 * 신청자 닉네임·프로필이 비어 오면 null 로 둔다. 서버가 프로필을 못 찾은 경우다 - 표시 문구는
 * 화면이 정한다.
 */
fun PendingPlaceDto.toPendingPlace(): PendingPlace = PendingPlace(
    id = id,
    placeName = name?.trim()?.takeIf { it.isNotBlank() },
    requesterName = createdByNickname?.trim()?.takeIf { it.isNotBlank() },
    requesterImageUrl = createdByProfileImageUrl?.trim()?.takeIf { it.isNotBlank() },
    requestedAtMillis = parseServerDateTime(createdAt),
)
