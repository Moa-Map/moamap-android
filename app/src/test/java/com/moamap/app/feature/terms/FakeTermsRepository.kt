package com.moamap.app.feature.terms

import com.moamap.app.feature.terms.domain.model.Terms
import com.moamap.app.feature.terms.domain.model.TermsAgreementStatus
import com.moamap.app.feature.terms.domain.model.TermsDetail
import com.moamap.app.feature.terms.domain.model.TermsKind
import com.moamap.app.feature.terms.domain.repository.TermsRepository

fun testTerms(termId: Long, code: String, kind: TermsKind = TermsKind.REQUIRED) =
    Terms(termId = termId, code = code, title = code, version = "v1", kind = kind)

/**
 * 정해 둔 값을 돌려주는 테스트용 약관 저장소.
 *
 * 각 `*Error` 를 채우면 그 호출이 던진다. 동의 상태는 [statuses] 를 앞에서부터 하나씩 꺼내고, 마지막 하나는 계속 쓴다.
 */
class FakeTermsRepository(
    var termsList: List<Terms> = emptyList(),
    statuses: List<TermsAgreementStatus> = listOf(TermsAgreementStatus(needsAgreement = false, pendingTerms = emptyList())),
) : TermsRepository {

    private val statuses = ArrayDeque(statuses)

    var listError: Exception? = null
    var statusError: Exception? = null
    var agreeError: Exception? = null
    var agreeResult = TermsAgreementStatus(needsAgreement = false, pendingTerms = emptyList())

    var statusCount = 0
        private set

    val agreedTermIds = mutableListOf<List<Long>>()

    override suspend fun getTermsList(): List<Terms> {
        listError?.let { throw it }
        return termsList
    }

    override suspend fun getTermsDetail(termId: Long): TermsDetail = TODO("사용하지 않음")

    override suspend fun getAgreementStatus(): TermsAgreementStatus {
        statusCount++
        statusError?.let { throw it }
        return if (statuses.size > 1) statuses.removeFirst() else statuses.first()
    }

    override suspend fun agree(termIds: List<Long>): TermsAgreementStatus {
        agreedTermIds += termIds
        agreeError?.let { throw it }
        return agreeResult
    }
}
