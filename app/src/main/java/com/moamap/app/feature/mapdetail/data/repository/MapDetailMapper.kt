package com.moamap.app.feature.mapdetail.data.repository

import com.moamap.app.feature.collection.data.remote.MapDetailDto
import com.moamap.app.feature.collection.domain.model.MapType
import com.moamap.app.feature.explore.data.remote.PlaceDto
import com.moamap.app.feature.mapdetail.domain.model.MapDetail
import com.moamap.app.feature.mapdetail.domain.model.MapPlace
import com.moamap.app.feature.mapdetail.domain.model.MapRole

/** 이름 없는 지도는 서버 데이터가 깨진 경우다. 제목 자리가 빈 줄로 보이지 않게 채운다. */
private const val UNTITLED_MAP = "이름 없는 지도"

private const val UNTITLED_PLACE = "이름 없는 장소"

private const val PRIVATE_TYPE = "PRIVATE"

private const val OFFICIAL_TYPE = "OFFICIAL"

/**
 * 지도 상세 응답을 도메인으로 옮긴다.
 *
 * 아는 값이 아니면 커뮤니티로 본다. 서버가 종류를 하나 더 늘려도 화면이 열리기는 해야 하고,
 * 커뮤니티가 권한이 가장 좁은 쪽은 아니지만 참여하지 않은 상태로는 볼 수만 있어 안전하다.
 */
fun MapDetailDto.toMapDetail(ownerName: String?): MapDetail = MapDetail(
    id = id,
    title = name?.takeIf { it.isNotBlank() } ?: UNTITLED_MAP,
    description = description?.takeIf { it.isNotBlank() },
    imageUrl = imageUrl?.takeIf { it.isNotBlank() },
    ownerName = ownerName?.takeIf { it.isNotBlank() },
    type = when (type) {
        PRIVATE_TYPE -> MapType.Private
        OFFICIAL_TYPE -> MapType.Official
        else -> MapType.Community
    },
    role = MapRole.from(myRole),
    tags = tags.filter { tag -> tag.isNotBlank() },
    memberCount = memberCount,
    placeCount = placeCount,
    joined = joined,
    personal = personal,
    inviteCode = inviteCode?.takeIf { code -> code.isNotBlank() },
)

/** 주소는 도로명을 우선한다. 사람이 읽기 쉬운 쪽이고 카드의 한 줄에도 잘 들어간다. */
fun PlaceDto.toMapPlace(): MapPlace = MapPlace(
    id = id,
    name = name?.takeIf { it.isNotBlank() } ?: UNTITLED_PLACE,
    address = roadAddress?.takeIf { it.isNotBlank() }
        ?: address?.takeIf { it.isNotBlank() }
        ?: "",
    latitude = lat,
    longitude = lng,
    photoUrl = photoUrls.firstOrNull { url -> url.isNotBlank() },
    description = description?.takeIf { it.isNotBlank() }.orEmpty(),
    category = category?.takeIf { it.isNotBlank() }.orEmpty(),
    tags = tags.map { tag -> tag.trim() }.filter { tag -> tag.isNotEmpty() },
    instagramUrl = instagramSourceUrl(sourceType, sourceUrl),
    // 아직 아무도 안 매긴 장소는 avgRating 이 null 로 온다. 화면은 0.0 으로 읽는다.
    rating = avgRating ?: 0.0,
    reviewCount = commentCount,
    kakaoPlaceId = kakaoPlaceId?.trim().orEmpty(),
    likeCount = likeCount,
    liked = likedByMe,
)

private const val INSTAGRAM_SOURCE_TYPE = "INSTAGRAM"

/**
 * 인스타그램에서 가져온 장소의 원본 주소. 서버는 가져올 때 앱이 보낸 게시물 주소를 그대로 저장한다.
 *
 * 화면이 이 주소를 그대로 열기 때문에 웹 주소만 남긴다. `intent:` 같은 다른 형식이 섞여 오면
 * 다른 앱을 엉뚱하게 띄울 수 있다.
 */
internal fun instagramSourceUrl(sourceType: String?, sourceUrl: String?): String? {
    if (sourceType != INSTAGRAM_SOURCE_TYPE) return null
    val url = sourceUrl?.trim() ?: return null
    val web = url.startsWith("https://", ignoreCase = true) || url.startsWith("http://", ignoreCase = true)
    return url.takeIf { web }
}
