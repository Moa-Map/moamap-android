package com.moamap.app.feature.terms.data

import com.moamap.app.core.network.EnvelopeConverterFactory
import com.moamap.app.feature.terms.data.remote.TermsService
import com.moamap.app.feature.terms.data.repository.TermsRepositoryImpl
import com.moamap.app.feature.terms.domain.model.Terms
import com.moamap.app.feature.terms.domain.model.TermsKind
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/** 약관 API 를 부르는 경로·본문과, 응답을 도메인 모델로 옮기는 매핑. */
class TermsServiceTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: TermsRepositoryImpl

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val json = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        }
        val service = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(EnvelopeConverterFactory(json))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(TermsService::class.java)
        repository = TermsRepositoryImpl(service)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `약관 목록을 화면 순서대로 읽고 kind 를 옮긴다`() = runTest {
        server.enqueue(
            MockResponse().setBody(
                """{"success":true,"data":[""" +
                    """{"termId":1,"code":"SERVICE","title":"서비스 이용약관","version":"2026-10-20","kind":"REQUIRED"},""" +
                    """{"termId":2,"code":"AGE","title":"만 14세 이상 확인","version":"2026-10-20","kind":"REQUIRED"},""" +
                    """{"termId":3,"code":"POLICY","title":"개인정보 처리방침","version":"2026-10-20","kind":"VIEW_ONLY"}]}""",
            ),
        )

        val terms = repository.getTermsList()

        assertEquals("/api/v1/terms", server.takeRequest().requestUrl?.encodedPath)
        assertEquals(listOf(1L, 2L, 3L), terms.map { it.termId })
        assertEquals(
            Terms(termId = 3, code = "POLICY", title = "개인정보 처리방침", version = "2026-10-20", kind = TermsKind.VIEW_ONLY),
            terms[2],
        )
        assertEquals(TermsKind.REQUIRED, terms[0].kind)
    }

    /** 필수로 잘못 읽으면 동의할 수 없는 약관에 체크를 요구하게 된다. */
    @Test
    fun `모르는 kind 는 보기만 하는 약관으로 읽는다`() = runTest {
        server.enqueue(
            MockResponse().setBody(
                """{"success":true,"data":[{"termId":9,"code":"NEW","title":"새 약관","version":"1","kind":"OPTIONAL"}]}""",
            ),
        )

        assertEquals(TermsKind.VIEW_ONLY, repository.getTermsList().single().kind)
    }

    @Test
    fun `전문은 약관 번호 경로로 부르고 마크다운 본문과 active 를 읽는다`() = runTest {
        server.enqueue(
            MockResponse().setBody(
                """{"success":true,"data":{"termId":1,"code":"SERVICE","title":"서비스 이용약관",""" +
                    """"version":"2026-10-20","kind":"REQUIRED","active":false,"content":"# 서비스 이용약관\n**굵게**"}}""",
            ),
        )

        val detail = repository.getTermsDetail(1)

        assertEquals("/api/v1/terms/1", server.takeRequest().requestUrl?.encodedPath)
        assertEquals("서비스 이용약관", detail.title)
        assertEquals("# 서비스 이용약관\n**굵게**", detail.content)
        assertFalse(detail.active)
    }

    @Test
    fun `동의 상태는 남은 필수 약관과 함께 읽는다`() = runTest {
        server.enqueue(
            MockResponse().setBody(
                """{"success":true,"data":{"needsAgreement":true,"pendingTerms":[""" +
                    """{"termId":2,"code":"AGE","title":"만 14세 이상 확인","version":"2026-10-20","kind":"REQUIRED"}]}}""",
            ),
        )

        val status = repository.getAgreementStatus()

        assertEquals("/api/v1/terms/agreements/me", server.takeRequest().requestUrl?.encodedPath)
        assertTrue(status.needsAgreement)
        assertEquals(listOf(2L), status.pendingTerms.map { it.termId })
    }

    @Test
    fun `동의는 약관 번호 목록을 POST 로 보내고 동의한 뒤의 상태를 읽는다`() = runTest {
        server.enqueue(
            MockResponse().setBody("""{"success":true,"data":{"needsAgreement":false,"pendingTerms":[]}}"""),
        )

        val status = repository.agree(listOf(1, 2))

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/terms/agreements", request.requestUrl?.encodedPath)
        assertEquals("""{"termIds":[1,2]}""", request.body.readUtf8())
        assertFalse(status.needsAgreement)
        assertTrue(status.pendingTerms.isEmpty())
    }
}
