package com.moamap.app.feature.mapdetail.presentation.logs

import com.moamap.app.feature.mapdetail.domain.model.PendingPlace
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PendingRequestUiModelTest {

    private val now = 1_755_500_000_000L

    private fun pending(
        id: Long = 101L,
        placeName: String? = "성수 브루어리",
        requesterName: String? = null,
        requesterImageUrl: String? = null,
        requestedAtMillis: Long? = now - 30 * 60 * 1000L,
    ) = PendingPlace(
        id = id,
        placeName = placeName,
        requesterId = 3L,
        requesterName = requesterName,
        requesterImageUrl = requesterImageUrl,
        requestedAtMillis = requestedAtMillis,
    )

    @Test
    fun `요청을 카드 모델로 옮긴다`() {
        val card = listOf(pending()).toPendingRequestUiModels(now).single()

        assertEquals(101L, card.id)
        assertEquals("‘성수 브루어리’ 를 이 지도에 추가하고 싶어요", card.message)
        assertEquals("30분 전", card.timeAgo)
    }

    /** 받침에 따라 조사가 갈린다. 활동 내역이 쓰는 규칙을 그대로 쓴다. */
    @Test
    fun `받침이 있으면 을 을 쓴다`() {
        val card = listOf(pending(placeName = "연남 책방")).toPendingRequestUiModels(now).single()

        assertEquals("‘연남 책방’ 을 이 지도에 추가하고 싶어요", card.message)
    }

    /** 빈 따옴표(‘’)가 남으면 지워진 장소처럼 보인다. */
    @Test
    fun `장소 이름이 없으면 이름을 뺀 문장을 쓴다`() {
        val card = listOf(pending(placeName = null)).toPendingRequestUiModels(now).single()

        assertEquals("장소를 이 지도에 추가하고 싶어요", card.message)
    }

    /**
     * 서버가 아직 신청자 닉네임을 내려주지 않는다.
     *
     * 이름 줄이 빈 채로 남으면 카드가 깨져 보여, 활동 내역이 쓰는 문구로 자리를 채운다.
     */
    @Test
    fun `신청자 이름이 없으면 대체 문구를 쓴다`() {
        val card = listOf(pending()).toPendingRequestUiModels(now).single()

        assertEquals(UNKNOWN_REQUESTER, card.userName)
        assertNull(card.userImageUrl)
    }

    /** 서버가 닉네임을 내려주기 시작하면 그대로 쓴다. */
    @Test
    fun `신청자 이름이 있으면 그대로 쓴다`() {
        val card = listOf(pending(requesterName = "박지훈", requesterImageUrl = "https://cdn/3.jpg"))
            .toPendingRequestUiModels(now)
            .single()

        assertEquals("박지훈", card.userName)
        assertEquals("https://cdn/3.jpg", card.userImageUrl)
    }

    @Test
    fun `시각을 모르면 시각 자리를 비운다`() {
        val card = listOf(pending(requestedAtMillis = null)).toPendingRequestUiModels(now).single()

        assertEquals("", card.timeAgo)
    }
}
