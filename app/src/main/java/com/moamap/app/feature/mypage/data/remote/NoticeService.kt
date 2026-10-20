package com.moamap.app.feature.mypage.data.remote

import com.moamap.app.core.network.model.PageResponse
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * 공지사항 조회. 로그인 없이도 받는다.
 *
 * [UserService] 와 같은 user-service 지만 따로 둔다. 테스트가 [UserService] 를 흉내 내는 곳이 있어 메서드를
 * 늘리면 그쪽을 함께 고쳐야 한다.
 */
interface NoticeService {

    /** 서버가 최신 등록순으로 고정해 준다. 한 번에 최대 50개. */
    @GET("api/v1/notices")
    suspend fun getNotices(
        @Query("page") page: Int,
        @Query("size") size: Int,
    ): PageResponse<NoticeDto>

    /** 지워졌거나 없는 공지는 404 `USER_011`. */
    @GET("api/v1/notices/{noticeId}")
    suspend fun getNotice(@Path("noticeId") noticeId: Long): NoticeDto
}

/** 공지 한 건. 목록에도 본문 전체가 온다(미리보기 자르기는 앱이 한다). 본문은 일반 텍스트다. */
@Serializable
data class NoticeDto(
    val noticeId: Long = 0,
    val title: String? = null,
    val content: String? = null,
    /** `Z` 가 붙은 UTC 시각. */
    val createdAt: String? = null,
)
