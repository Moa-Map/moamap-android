package com.moamap.app.feature.officialmap.domain.model

import androidx.compose.runtime.Immutable

/**
 * 공식지도 목록에 그려지는 지도 한 건.
 *
 * 인원·장소 수는 숫자로 들고 표기는 화면에서 만든다. `MyMap` 과 같은 방식이다.
 */
@Immutable
data class OfficialMap(
    val id: Long,
    val title: String,
    val description: String,
    /** 대표 이미지. null 이면 카드가 로고 기본 이미지를 그린다. */
    val imageUrl: String?,
    val memberCount: Int,
    val placeCount: Int,
    val joined: Boolean,
)

/**
 * 유동인구 지도의 서버 이름.
 *
 * 서버가 특수 공식지도를 따로 표시해 주지 않아 이름으로 가린다(백엔드 시드 주석이 정한 방식).
 * 서버에서 이름을 바꾸면 이 값도 같이 바꿔야 한다.
 */
private const val DENSITY_OFFICIAL_MAP_NAME = "유동인구 지도"

/**
 * 유동인구 지도인가. 장소 대신 실시간 밀집도를 보여 주는 지도라 지도 상세가 아니라 유동인구
 * 화면으로 연다.
 *
 * 공식지도인지도 함께 본다. 사용자가 커뮤니티 지도 이름을 같게 지어도 넘어가면 안 된다.
 */
fun isDensityOfficialMap(official: Boolean, title: String): Boolean =
    official && title == DENSITY_OFFICIAL_MAP_NAME

/** 공중화장실 지도의 서버 이름. 유동인구 지도와 같은 이유로 이름으로 가린다. */
private const val RESTROOM_OFFICIAL_MAP_NAME = "공중화장실 지도"

/**
 * 공중화장실 지도인가. 화장실은 장소가 아니라 따로 내려오는 공공데이터라 지도 상세가 아니라
 * 화장실 화면으로 연다.
 */
fun isRestroomOfficialMap(official: Boolean, title: String): Boolean =
    official && title == RESTROOM_OFFICIAL_MAP_NAME
