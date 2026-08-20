package com.moamap.app.feature.mapdetail.data.repository

import com.moamap.app.feature.explore.data.remote.PlaceDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PendingPlaceMapperTest {

    @Test
    fun `승인 대기 응답을 도메인으로 옮긴다`() {
        val pending = PlaceDto(
            id = 101,
            name = "성수 브루어리",
            createdBy = 3,
            status = "PENDING",
            createdAt = "2026-08-18T09:30:00",
        ).toPendingPlace()

        assertEquals(101L, pending.id)
        assertEquals("성수 브루어리", pending.placeName)
        assertEquals(3L, pending.requesterId)
        assertEquals(parseServerDateTime("2026-08-18T09:30:00"), pending.requestedAtMillis)
    }

    /** 이름 자리가 비면 화면이 빈 따옴표를 그린다. 없는 것과 같이 취급한다. */
    @Test
    fun `장소 이름이 비면 없는 것으로 본다`() {
        assertNull(PlaceDto(id = 1, name = "   ").toPendingPlace().placeName)
        assertNull(PlaceDto(id = 1, name = null).toPendingPlace().placeName)
    }

    @Test
    fun `시각을 못 읽으면 비운다`() {
        assertNull(PlaceDto(id = 1, createdAt = "어제").toPendingPlace().requestedAtMillis)
        assertNull(PlaceDto(id = 1, createdAt = null).toPendingPlace().requestedAtMillis)
    }

    /**
     * 서버가 아직 신청자 닉네임·프로필을 내려주지 않는다.
     *
     * 자리를 비워 두고 화면이 대신 채운다. 서버가 필드를 더하면 이 단언이 깨지므로, 그때
     * 매퍼와 함께 고치라는 표시이기도 하다.
     */
    @Test
    fun `신청자 이름과 사진은 아직 비어 있다`() {
        val pending = PlaceDto(id = 1, createdBy = 3).toPendingPlace()

        assertNull(pending.requesterName)
        assertNull(pending.requesterImageUrl)
    }
}
