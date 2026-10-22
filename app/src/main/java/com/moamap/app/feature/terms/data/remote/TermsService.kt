package com.moamap.app.feature.terms.data.remote

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * 약관 API(백엔드 PR #150).
 *
 * 목록·전문은 로그인 없이 받고, 동의 상태·동의는 로그인한 세션으로 부른다. 오류 코드는 `USER_020` 없는 약관(404),
 * `USER_021` 동의할 수 없는 약관(400), `USER_022` 필수 약관 누락(400), `USER_023` 그사이 약관 개정(409)이다.
 */
interface TermsService {

    /** 지금 보여 줄 약관, 화면 순서대로. */
    @GET("api/v1/terms")
    suspend fun getTerms(): List<TermsDto>

    @GET("api/v1/terms/{termId}")
    suspend fun getTermsDetail(@Path("termId") termId: Long): TermsDetailDto

    @GET("api/v1/terms/agreements/me")
    suspend fun getMyAgreement(): TermsAgreementDto

    /** 응답은 동의한 뒤의 상태로 [getMyAgreement] 와 같은 모양이다. */
    @POST("api/v1/terms/agreements")
    suspend fun agree(@Body request: TermsAgreeRequestDto): TermsAgreementDto
}

/** 약관 하나. `kind` 는 `REQUIRED` 또는 `VIEW_ONLY`. */
@Serializable
data class TermsDto(
    val termId: Long = 0,
    val code: String? = null,
    val title: String? = null,
    val version: String? = null,
    val kind: String? = null,
)

/** 약관 전문. 본문은 마크다운이다. */
@Serializable
data class TermsDetailDto(
    val termId: Long = 0,
    val code: String? = null,
    val title: String? = null,
    val version: String? = null,
    val kind: String? = null,
    val active: Boolean = true,
    val content: String? = null,
)

/** `pendingTerms` 는 아직 동의하지 않은 필수 약관. */
@Serializable
data class TermsAgreementDto(
    val needsAgreement: Boolean = false,
    val pendingTerms: List<TermsDto> = emptyList(),
)

@Serializable
data class TermsAgreeRequestDto(
    val termIds: List<Long>,
)
