package com.example.moamap.feature.mapdetail.presentation

import com.example.moamap.feature.collection.domain.model.MapType
import com.example.moamap.feature.mapdetail.domain.model.MapDetail
import com.example.moamap.feature.mapdetail.domain.model.MapPlace
import com.example.moamap.feature.mapdetail.domain.model.MapPlacePreview
import com.example.moamap.feature.mapdetail.domain.model.MapRole
import com.example.moamap.feature.mapdetail.domain.repository.MapDetailRepository
import kotlinx.coroutines.delay

internal fun testMap(
    id: Long = 1L,
    type: MapType = MapType.Community,
    role: MapRole = MapRole.None,
    joined: Boolean = false,
    memberCount: Int = 1,
    placeCount: Int = 0,
    personal: Boolean = false,
    inviteCode: String? = null,
) = MapDetail(
    id = id,
    title = "지도$id",
    description = null,
    imageUrl = null,
    ownerName = null,
    type = type,
    role = role,
    tags = emptyList(),
    memberCount = memberCount,
    placeCount = placeCount,
    joined = joined,
    personal = personal,
    inviteCode = inviteCode,
)

internal fun testPlace(id: Long) = MapPlace(
    id = id,
    name = "장소$id",
    address = "주소$id",
    latitude = 37.5 + id,
    longitude = 127.0 + id,
    photoUrl = null,
)

/**
 * 호출을 기록하는 가짜 저장소.
 *
 * [responseDelayMillis] 를 두면 요청이 진행 중인 상태를 만들 수 있다. 이중 탭 차단을
 * 검증하려면 앞선 요청이 실제로 매달려 있어야 한다.
 */
internal open class FakeMapDetailRepository(
    private val responseDelayMillis: Long = 0L,
    var map: () -> MapDetail = { testMap() },
    var places: () -> MapPlacePreview = { MapPlacePreview() },
    var allPlaces: () -> List<MapPlace> = { emptyList() },
) : MapDetailRepository {

    val calls = mutableListOf<String>()

    override suspend fun getMapDetail(mapId: Long): MapDetail {
        calls += "getMapDetail"
        delay(responseDelayMillis)
        return map()
    }

    override suspend fun getPlacePreview(mapId: Long, visibleCount: Int): MapPlacePreview {
        calls += "getPlacePreview($visibleCount)"
        delay(responseDelayMillis)
        return places()
    }

    override suspend fun getPlaces(mapId: Long): List<MapPlace> {
        calls += "getPlaces"
        delay(responseDelayMillis)
        return allPlaces()
    }

    override suspend fun joinMap(mapId: Long) {
        calls += "joinMap"
        delay(responseDelayMillis)
    }

    override suspend fun leaveMap(mapId: Long) {
        calls += "leaveMap"
        delay(responseDelayMillis)
    }

    override suspend fun deleteMap(mapId: Long) {
        calls += "deleteMap"
        delay(responseDelayMillis)
    }
}
