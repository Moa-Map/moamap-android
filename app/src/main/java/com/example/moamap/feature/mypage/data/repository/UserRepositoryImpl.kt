package com.example.moamap.feature.mypage.data.repository

import com.example.moamap.feature.mypage.data.remote.UpdateMyPageRequestDto
import com.example.moamap.feature.mypage.data.remote.UserService
import com.example.moamap.feature.mypage.domain.model.MyProfile
import com.example.moamap.feature.mypage.domain.repository.UserRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val userService: UserService,
) : UserRepository {

    override suspend fun getMyProfile(): MyProfile = userService.getMyPage().toMyProfile()

    // 나머지 필드는 기본값 null 이라 직렬화에서 빠진다. 부분 수정이므로 서버가 건드리지 않는다.
    override suspend fun updateMyProfile(nickname: String, introduction: String): MyProfile =
        userService.updateMyPage(
            UpdateMyPageRequestDto(nickname = nickname, introduction = introduction),
        ).toMyProfile()
}
