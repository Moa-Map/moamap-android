package com.example.moamap.feature.mapdetail.data.repository

import com.example.moamap.feature.collection.data.remote.MapDetailDto
import com.example.moamap.feature.collection.domain.model.MapType
import com.example.moamap.feature.explore.data.remote.PlaceDto
import com.example.moamap.feature.mapdetail.domain.model.MapDetail
import com.example.moamap.feature.mapdetail.domain.model.MapPlace
import com.example.moamap.feature.mapdetail.domain.model.MapRole

/** 이름 없는 지도는 서버 데이터가 깨진 경우다. 제목 자리가 빈 줄로 보이지 않게 채운다. */
private const val UNTITLED_MAP = "이름 없는 지도"

private const val UNTITLED_PLACE = "이름 없는 장소"

private const val PRIVATE_TYPE = "PRIVATE"

/**
 * 지도 상세 응답을 도메인으로 옮긴다.
 *
 * 서버 `type` 은 OFFICIAL 도 낼 수 있지만 공식 지도는 이 화면으로 들어오지 않는다.
 * PRIVATE 이 아닌 값은 모두 커뮤니티로 본다 - 화면의 판단 기준이 "프라이빗이냐" 뿐이라서다.
 */
fun MapDetailDto.toMapDetail(ownerName: String?): MapDetail = MapDetail(
    id = id,
    title = name?.takeIf { it.isNotBlank() } ?: UNTITLED_MAP,
    description = description?.takeIf { it.isNotBlank() },
    imageUrl = imageUrl?.takeIf { it.isNotBlank() },
    ownerName = ownerName?.takeIf { it.isNotBlank() },
    type = if (type == PRIVATE_TYPE) MapType.Private else MapType.Community,
    role = MapRole.from(myRole),
    tags = tags.filter { tag -> tag.isNotBlank() },
    memberCount = memberCount,
    placeCount = placeCount,
    joined = joined,
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
)
