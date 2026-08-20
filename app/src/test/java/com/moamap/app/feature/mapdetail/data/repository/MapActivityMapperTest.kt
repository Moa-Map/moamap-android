package com.moamap.app.feature.mapdetail.data.repository

import com.moamap.app.feature.explore.data.remote.PlaceActivityDto
import com.moamap.app.feature.mapdetail.domain.model.MapActivityType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class MapActivityMapperTest {

    @Test
    fun `서버 종류를 그대로 옮긴다`() {
        assertEquals(
            MapActivityType.PlaceAdded,
            PlaceActivityDto(type = "PLACE_ADDED").toMapActivity()?.type,
        )
        assertEquals(
            MapActivityType.PlaceRemoved,
            PlaceActivityDto(type = "PLACE_DELETED").toMapActivity()?.type,
        )
        assertEquals(
            MapActivityType.ReviewCreated,
            PlaceActivityDto(type = "REVIEW_CREATED").toMapActivity()?.type,
        )
    }

    @Test
    fun `모르는 종류는 버린다`() {
        assertNull(PlaceActivityDto(type = "PLACE_UPDATED").toMapActivity())
        assertNull(PlaceActivityDto(type = null).toMapActivity())
        assertNull(PlaceActivityDto(type = "").toMapActivity())
    }

    @Test
    fun `공백뿐인 이름과 장소명은 비워 둔다`() {
        val activity = PlaceActivityDto(
            type = "PLACE_ADDED",
            actorNickname = "   ",
            actorProfileImageUrl = " ",
            placeName = "  ",
        ).toMapActivity()

        assertNotNull(activity)
        assertNull(activity?.actorName)
        assertNull(activity?.actorImageUrl)
        assertNull(activity?.placeName)
    }

    @Test
    fun `이름과 장소명의 앞뒤 공백을 턴다`() {
        val activity = PlaceActivityDto(
            type = "PLACE_ADDED",
            actorNickname = " 김도현 ",
            placeName = " 어니언 성수 ",
        ).toMapActivity()

        assertEquals("김도현", activity?.actorName)
        assertEquals("어니언 성수", activity?.placeName)
    }

    @Test
    fun `별점은 별 다섯 칸을 벗어나지 않는다`() {
        assertEquals(5, PlaceActivityDto(type = "REVIEW_CREATED", rating = 9).toMapActivity()?.rating)
        assertEquals(0, PlaceActivityDto(type = "REVIEW_CREATED", rating = -1).toMapActivity()?.rating)
        assertNull(PlaceActivityDto(type = "PLACE_ADDED", rating = null).toMapActivity()?.rating)
    }

    @Test
    fun `읽을 수 없는 시각은 비운다`() {
        assertNull(PlaceActivityDto(type = "PLACE_ADDED", occurredAt = null).toMapActivity()?.occurredAtMillis)
        assertNotNull(
            PlaceActivityDto(type = "PLACE_ADDED", occurredAt = "2026-07-30T02:54:12")
                .toMapActivity()?.occurredAtMillis,
        )
    }
}
