package com.example.moamap.feature.mypage.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH

interface UserService {

    @GET("api/v1/users/me")
    suspend fun getMyPage(): MyPageDto

    @PATCH("api/v1/users/me")
    suspend fun updateMyPage(@Body request: UpdateMyPageRequestDto): MyPageDto
}
