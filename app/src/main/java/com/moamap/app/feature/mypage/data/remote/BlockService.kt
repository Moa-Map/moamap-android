package com.moamap.app.feature.mypage.data.remote

import kotlinx.serialization.Serializable
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * 사용자 차단. user-service 지만 [NoticeService] 와 같은 이유로 [UserService] 와 따로 둔다.
 */
interface BlockService {

    /** 최근 차단한 순. 탈퇴한 사람은 빠진다. 페이지를 나누지 않는다. */
    @GET("api/v1/users/me/blocks")
    suspend fun getBlockedUsers(): List<BlockedUserDto>

    /** 이미 차단한 사람을 또 차단해도 성공한다. 나 자신은 400 `USER_026`, 없는 사람은 404. */
    @POST("api/v1/users/{userId}/blocks")
    suspend fun block(@Path("userId") userId: Long)

    /** 차단하지 않은 사람을 해제해도 성공한다. */
    @DELETE("api/v1/users/{userId}/blocks")
    suspend fun unblock(@Path("userId") userId: Long)
}

@Serializable
data class BlockedUserDto(
    val userId: Long = 0,
    val nickname: String? = null,
    val profileImageUrl: String? = null,
)
