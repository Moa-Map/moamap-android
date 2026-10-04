package com.moamap.app.feature.terms.data.repository

import android.content.Context
import androidx.annotation.RawRes
import com.moamap.app.R
import com.moamap.app.feature.terms.domain.model.Terms
import com.moamap.app.feature.terms.domain.model.TermsCode
import com.moamap.app.feature.terms.domain.model.TermsConsentType
import com.moamap.app.feature.terms.domain.repository.TermsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * 앱에 넣어 둔 약관 버전. 본문을 고치면 올린다.
 *
 * 백엔드 약관 API 가 생기기 전까지 쓰는 값이라 약관마다 따로 두지 않고 하나로 둔다.
 */
internal const val BUNDLED_TERMS_VERSION = "2026-10-09"

/** 앱에 넣어 둔 약관 하나. 본문은 `res/raw` 의 마크다운 파일이다. */
private data class BundledTerms(
    val code: String,
    val title: String,
    val consentType: TermsConsentType,
    @RawRes val contentRes: Int,
)

/** 동의 화면 순서(시안 「이용약관 동의」)대로. 개인정보처리방침은 동의 화면에 나오지 않는다. */
private val BundledTermsList = listOf(
    BundledTerms(TermsCode.SERVICE, "서비스 이용약관", TermsConsentType.REQUIRED, R.raw.terms_service),
    BundledTerms(TermsCode.PRIVACY_COLLECTION, "개인정보수집 및 이용동의", TermsConsentType.REQUIRED, R.raw.terms_privacy_collection),
    BundledTerms(TermsCode.SENSITIVE_INFO, "민감정보수집 동의약관", TermsConsentType.REQUIRED, R.raw.terms_sensitive),
    BundledTerms(TermsCode.THIRD_PARTY, "개인정보 제3자 제공동의", TermsConsentType.REQUIRED, R.raw.terms_third_party),
    BundledTerms(TermsCode.MARKETING, "마케팅활용 동의", TermsConsentType.OPTIONAL, R.raw.terms_marketing),
    BundledTerms(TermsCode.PRIVACY_POLICY, "개인정보처리방침", TermsConsentType.NOTICE, R.raw.terms_privacy_policy),
)

/**
 * 앱에 넣어 둔 약관을 읽는다. 본문은 백엔드가 약관을 마크다운으로 줄 예정이라 같은 형식의 파일로 둔다.
 * 약관 API 가 생기면 이 클래스 대신 서버에서 받는 구현을 묶는다.
 */
class BundledTermsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) : TermsRepository {

    override suspend fun getAgreementTerms(): List<Terms> =
        BundledTermsList
            .filter { terms -> terms.consentType != TermsConsentType.NOTICE }
            .map { terms -> read(terms) }

    override suspend fun getTerms(code: String): Terms =
        read(BundledTermsList.first { terms -> terms.code == code })

    private suspend fun read(terms: BundledTerms): Terms = withContext(Dispatchers.IO) {
        Terms(
            code = terms.code,
            title = terms.title,
            consentType = terms.consentType,
            version = BUNDLED_TERMS_VERSION,
            content = context.resources.openRawResource(terms.contentRes)
                .bufferedReader()
                .use { reader -> reader.readText() },
        )
    }
}
