package com.moamap.app.feature.notification.data.remote

import com.moamap.app.core.network.model.PageResponse
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * 내 알림(백엔드 #139). 로그인이 필요하다. 남의 알림은 없는 알림과 같이 404 다.
 *
 * 안 읽은 수(`unread-count`)와 삭제는 화면에 자리가 없어 아직 부르지 않는다(10-26 사용자 결정).
 */
interface NotificationService {

    /** 최신순. 한 번에 최대 50개. 90일이 지난 알림은 서버가 지운다. */
    @GET("api/v1/notifications")
    suspend fun getNotifications(
        @Query("page") page: Int,
        @Query("size") size: Int,
    ): PageResponse<NotificationDto>

    /** 이미 읽은 알림이어도 성공한다. */
    @PATCH("api/v1/notifications/{notificationId}/read")
    suspend fun markRead(@Path("notificationId") notificationId: Long)

    /** 안 읽은 알림이 없어도 성공한다. */
    @PATCH("api/v1/notifications/read-all")
    suspend fun markAllRead()
}

/** 알림 한 건. 문구([message])는 서버가 만든다. */
@Serializable
data class NotificationDto(
    val notificationId: Long = 0,
    /** `PLACE_APPROVED`·`MAP_POST_COMMENTED` 등. 앱이 모르는 값이 올 수 있다. */
    val type: String? = null,
    val message: String? = null,
    /** 눌렀을 때 열 대상: `PLACE`·`MAP`·`MAP_POST`·`INQUIRY`·`REPORT`. */
    val targetType: String? = null,
    val targetId: Long? = null,
    /** 대상이 속한 지도. 없으면 null. */
    val mapId: Long? = null,
    val read: Boolean = false,
    /** `Z` 가 붙은 UTC 시각. */
    val createdAt: String? = null,
)
