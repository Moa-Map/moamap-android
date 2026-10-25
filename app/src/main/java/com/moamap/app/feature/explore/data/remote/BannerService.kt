package com.moamap.app.feature.explore.data.remote

import kotlinx.serialization.Serializable
import retrofit2.http.GET

/** 홈 맨 위 배너. 로그인 없이 받는다(백엔드 #132). */
interface BannerService {

    /** 노출 순서대로 최대 10개. 없으면 빈 목록이다. */
    @GET("api/v1/banners")
    suspend fun getBanners(): List<BannerDto>
}

/** 배너 한 장. 문구·버튼은 그림 안에 들어 있다. */
@Serializable
data class BannerDto(
    val bannerId: Long = 0,
    val imageUrl: String? = null,
)
