package com.moamap.app.feature.mypage.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
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

    /**
     * 프로필 이미지 업로드용 presigned PUT URL 을 발급받는다.
     *
     * 사진은 우리 서버로 올리지 않는다. 여기서 받은 주소로 앱이 직접 올리고, 응답의 `fileUrl`
     * 을 마이페이지 수정 요청의 `profileImageUrl` 에 담는다.
     *
     * 한 장만 발급한다. 허용 형식은 jpeg/png/webp, 최대 10MB.
     */
    @POST("api/v1/users/profile-upload-url")
    suspend fun createProfileUploadUrl(
        @Body request: ProfileUploadUrlRequestDto,
    ): ProfileUploadUrlDto
}
