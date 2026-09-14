package com.moamap.app.feature.mapdetail.data.repository

import com.moamap.app.feature.explore.data.remote.PendingPlaceDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PendingPlaceMapperTest {

    @Test
    fun `승인 대기 응답을 도메인으로 옮긴다`() {
        val pending = PendingPlaceDto(
            id = 101,
            name = "성수 브루어리",
            createdByNickname = "박지훈",
            createdByProfileImageUrl = "https://cdn/3.jpg",
            createdAt = "2026-08-18T09:30:00",
        ).toPendingPlace()

        assertEquals(101L, pending.id)
        assertEquals("성수 브루어리", pending.placeName)
        assertEquals("박지훈", pending.requesterName)
        assertEquals("https://cdn/3.jpg", pending.requesterImageUrl)
        assertEquals(parseServerDateTime("2026-08-18T09:30:00"), pending.requestedAtMillis)
    }

    /** 이름 자리가 비면 화면이 빈 따옴표를 그린다. 없는 것과 같이 취급한다. */
    @Test
    fun `장소 이름이 비면 없는 것으로 본다`() {
        assertNull(PendingPlaceDto(id = 1, name = "   ").toPendingPlace().placeName)
        assertNull(PendingPlaceDto(id = 1, name = null).toPendingPlace().placeName)
    }

    @Test
    fun `시각을 못 읽으면 비운다`() {
        assertNull(PendingPlaceDto(id = 1, createdAt = "어제").toPendingPlace().requestedAtMillis)
        assertNull(PendingPlaceDto(id = 1, createdAt = null).toPendingPlace().requestedAtMillis)
    }

    /** 서버가 신청자 프로필을 못 찾으면 둘 다 null 로 온다. 화면이 대체 문구를 채운다. */
    @Test
    fun `신청자 이름과 사진이 비면 없는 것으로 본다`() {
        val missing = PendingPlaceDto(id = 1).toPendingPlace()
        val blank = PendingPlaceDto(
            id = 1,
            createdByNickname = " ",
            createdByProfileImageUrl = "",
        ).toPendingPlace()

        assertNull(missing.requesterName)
        assertNull(missing.requesterImageUrl)
        assertNull(blank.requesterName)
        assertNull(blank.requesterImageUrl)
    }
}
