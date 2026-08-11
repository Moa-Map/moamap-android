package com.example.moamap.feature.mapdetail.data.repository

import com.example.moamap.feature.explore.data.remote.PlaceActivityDto
import com.example.moamap.feature.mapdetail.domain.model.MapActivity
import com.example.moamap.feature.mapdetail.domain.model.MapActivityType

private const val TYPE_PLACE_ADDED = "PLACE_ADDED"
private const val TYPE_PLACE_DELETED = "PLACE_DELETED"
private const val TYPE_REVIEW_CREATED = "REVIEW_CREATED"

/**
 * 모르는 종류는 null 이라 목록에서 빠진다.
 *
 * 서버에 이벤트 종류가 늘어도 앱은 무슨 말을 써야 할지 모른다. 그 자리에 빈 카드를 그리느니
 * 없는 것처럼 두는 편이 낫다. 종류를 enum 으로 역직렬화하지 않는 이유도 같다 - 모르는 값이
 * 하나 섞이면 응답 전체가 터진다.
 */
fun PlaceActivityDto.toMapActivity(): MapActivity? {
    val activityType = when (type) {
        TYPE_PLACE_ADDED -> MapActivityType.PlaceAdded
        TYPE_PLACE_DELETED -> MapActivityType.PlaceRemoved
        TYPE_REVIEW_CREATED -> MapActivityType.ReviewCreated
        else -> return null
    }

    return MapActivity(
        type = activityType,
        occurredAtMillis = parseServerDateTime(occurredAt),
        // 서버는 이름을 못 찾으면 "알 수 없음" 을 넣어 보낸다. 그 문구까지 여기서 정하지
        // 않고 비워 둔 것과 같이 취급한다 - 표시 문구는 화면이 정한다.
        actorName = actorNickname?.trim()?.takeIf { name -> name.isNotBlank() },
        actorImageUrl = actorProfileImageUrl?.trim()?.takeIf { url -> url.isNotBlank() },
        placeId = placeId,
        placeName = placeName?.trim()?.takeIf { name -> name.isNotBlank() },
        // 별 다섯 칸을 벗어난 옛 데이터가 문구에 그대로 찍히지 않게 한다.
        rating = rating?.coerceIn(0, 5),
    )
}
