package com.example.moamap.feature.mypage.data.repository

import com.example.moamap.feature.mypage.data.remote.MyPageDto
import com.example.moamap.feature.mypage.domain.model.MyProfile

internal fun MyPageDto.toMyProfile(): MyProfile = MyProfile(
    id = id,
    nickname = nickname.orEmpty(),
    email = email.orEmpty(),
    // 빈 문자열을 AsyncImage 에 넘기면 실패한 요청으로 잡혀 로그만 더러워진다. 없는 것과 같게 둔다.
    profileImageUrl = profileImageUrl?.takeIf { it.isNotBlank() },
    introduction = introduction.orEmpty(),
)
