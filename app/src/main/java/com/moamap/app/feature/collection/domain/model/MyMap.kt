package com.moamap.app.feature.collection.domain.model

import androidx.compose.runtime.Immutable
import com.moamap.app.feature.mapdetail.domain.model.MapRole

/** 모음 화면 목록에 그려지는 내 지도 한 건. */
@Immutable
data class MyMap(
    val id: Long,
    val title: String,
    val imageUrl: String?,
    val memberCount: Int,
    val placeCount: Int,
    /** 공식 인증 배지. 서버가 커뮤니티 탭 목록에 참여한 공식지도도 함께 담아 준다. */
    val official: Boolean,
    /** 로그인할 때 기본으로 생기는 개인 지도. 프라이빗 탭에서 따로 묶는다. */
    val personal: Boolean,
    /** 이 지도에서의 내 역할. 커뮤니티 카드의 방장·관리자 뱃지에 쓴다. */
    val role: MapRole = MapRole.None,
)
