package com.example.moamap.feature.mypage.domain.repository

import com.example.moamap.feature.mypage.domain.model.MyProfile

interface UserRepository {

    /** 내 프로필. 프로필 편집 화면에 들어올 때 한 번 부른다. */
    suspend fun getMyProfile(): MyProfile

    /**
     * 이름과 자기소개를 고치고 갱신된 프로필을 돌려준다.
     * 이메일은 소셜 로그인이 정하는 값이라 화면에서 고칠 수 없다.
     */
    suspend fun updateMyProfile(nickname: String, introduction: String): MyProfile
}
