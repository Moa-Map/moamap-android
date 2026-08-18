package com.example.moamap.feature.mapdetail.data.repository

import com.example.moamap.feature.explore.data.remote.PlaceDto
import com.example.moamap.feature.mapdetail.domain.model.PendingPlace

/**
 * 승인 대기 응답을 도메인으로 옮긴다.
 *
 * 신청자 닉네임·프로필은 채우지 않는다. 서버가 `createdBy` 숫자만 주기 때문이다. 앱에서
 * `GET /users/profiles` 로 따로 조회하는 방법도 있지만, 서버가 응답에 넣어 주기로 해서
 * 그때까지 비워 둔다
 */
fun PlaceDto.toPendingPlace(): PendingPlace = PendingPlace(
    id = id,
    placeName = name?.trim()?.takeIf { it.isNotBlank() },
    requesterId = createdBy,
    requesterName = null,
    requesterImageUrl = null,
    requestedAtMillis = parseServerDateTime(createdAt),
)
