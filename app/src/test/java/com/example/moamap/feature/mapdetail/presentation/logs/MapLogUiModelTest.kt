package com.example.moamap.feature.mapdetail.presentation.logs

import com.example.moamap.feature.mapdetail.domain.model.MapActivity
import com.example.moamap.feature.mapdetail.domain.model.MapActivityType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private const val NOW = 1_800_000_000_000L

class MapLogUiModelTest {

    @Test
    fun `받침이 있으면 을 없으면 를 을 붙인다`() {
        assertEquals(
            "‘성수 감자탕’ 을 추가했어요",
            messageOf(MapActivityType.PlaceAdded, placeName = "성수 감자탕"),
        )
        assertEquals(
            "‘어니언 성수’ 를 추가했어요",
            messageOf(MapActivityType.PlaceAdded, placeName = "어니언 성수"),
        )
    }

    @Test
    fun `한글로 끝나지 않는 이름은 를 로 둔다`() {
        assertEquals(
            "‘Onion’ 를 추가했어요",
            messageOf(MapActivityType.PlaceAdded, placeName = "Onion"),
        )
    }

    @Test
    fun `삭제 로그는 지도에서 뺐다고 적는다`() {
        assertEquals(
            "‘대림창고’ 를 지도에서 삭제했어요",
            messageOf(MapActivityType.PlaceRemoved, placeName = "대림창고"),
        )
    }

    @Test
    fun `후기 로그는 별점을 함께 적는다`() {
        assertEquals(
            "‘대림창고’ 에 별점 4점 후기를 남겼어요",
            messageOf(MapActivityType.ReviewCreated, placeName = "대림창고", rating = 4),
        )
    }

    @Test
    fun `별점이 없으면 그 자리를 비운다`() {
        assertEquals(
            "‘대림창고’ 에 후기를 남겼어요",
            messageOf(MapActivityType.ReviewCreated, placeName = "대림창고", rating = null),
        )
    }

    /** 빈 따옴표(`‘’`)가 남으면 이름이 지워진 장소처럼 보인다. */
    @Test
    fun `장소명이 없으면 이름을 뺀 문장으로 바꾼다`() {
        assertEquals("장소를 추가했어요", messageOf(MapActivityType.PlaceAdded, placeName = null))
        assertEquals(
            "장소를 지도에서 삭제했어요",
            messageOf(MapActivityType.PlaceRemoved, placeName = null),
        )
        assertEquals(
            "별점 5점 후기를 남겼어요",
            messageOf(MapActivityType.ReviewCreated, placeName = null, rating = 5),
        )
    }

    @Test
    fun `이름을 못 얻은 사용자도 자리를 비우지 않는다`() {
        val model = listOf(activity(actorName = null)).toMapLogUiModels(NOW).first()

        assertEquals("알 수 없는 사용자", model.userName)
    }

    @Test
    fun `시각으로 상대 표시를 만든다`() {
        val twoHoursAgo = NOW - 2 * 60 * 60 * 1000L
        val model = listOf(activity(occurredAtMillis = twoHoursAgo)).toMapLogUiModels(NOW).first()

        assertEquals("2시간 전", model.timeAgo)
    }

    /** 키가 겹치면 `LazyColumn` 이 터진다. 서버가 식별자를 주지 않아 직접 확인해 둔다. */
    @Test
    fun `값이 모두 같은 두 건도 키가 겹치지 않는다`() {
        val same = activity()
        val ids = listOf(same, same, same).toMapLogUiModels(NOW).map { model -> model.id }

        assertEquals(3, ids.toSet().size)
    }

    @Test
    fun `장소나 시각이 없어도 키를 만든다`() {
        val ids = listOf(activity(placeId = null, occurredAtMillis = null))
            .toMapLogUiModels(NOW)
            .map { model -> model.id }

        assertTrue(ids.single().isNotBlank())
    }

    private fun messageOf(
        type: MapActivityType,
        placeName: String?,
        rating: Int? = null,
    ): String = listOf(activity(type = type, placeName = placeName, rating = rating))
        .toMapLogUiModels(NOW)
        .first()
        .message

    private fun activity(
        type: MapActivityType = MapActivityType.PlaceAdded,
        occurredAtMillis: Long? = NOW,
        actorName: String? = "김도현",
        placeId: Long? = 1L,
        placeName: String? = "어니언 성수",
        rating: Int? = null,
    ) = MapActivity(
        type = type,
        occurredAtMillis = occurredAtMillis,
        actorName = actorName,
        actorImageUrl = null,
        placeId = placeId,
        placeName = placeName,
        rating = rating,
    )
}
