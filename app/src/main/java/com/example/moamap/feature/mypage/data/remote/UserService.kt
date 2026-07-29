package com.example.moamap.feature.mypage.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Query

interface UserService {

    @GET("api/v1/users/me")
    suspend fun getMyPage(): MyPageDto

    @PATCH("api/v1/users/me")
    suspend fun updateMyPage(@Body request: UpdateMyPageRequestDto): MyPageDto

    /**
     * 공개 프로필 벌크 조회.
     *
     * 서버가 한 번에 1~100 개까지 받는다. `ids=1&ids=2` 형태로 펼쳐져 나간다.
     */
    @GET("api/v1/users/profiles")
    suspend fun getProfiles(@Query("ids") ids: List<Long>): List<UserProfileDto>
}
