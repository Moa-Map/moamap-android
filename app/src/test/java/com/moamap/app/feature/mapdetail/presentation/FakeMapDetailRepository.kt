package com.moamap.app.feature.mapdetail.presentation

import com.moamap.app.feature.collection.domain.model.MapType
import com.moamap.app.feature.mapdetail.domain.model.MapDetail
import com.moamap.app.feature.mapdetail.domain.model.MapPlace
import com.moamap.app.feature.mapdetail.domain.model.MapRole
import com.moamap.app.feature.mapdetail.domain.model.PlaceLike
import com.moamap.app.feature.mapdetail.domain.repository.MapDetailRepository
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

internal fun testPlace(id: Long, liked: Boolean = false, likeCount: Int = 0) = MapPlace(
    id = id,
    name = "장소$id",
    address = "주소$id",
    latitude = 37.5 + id,
    longitude = 127.0 + id,
    photoUrl = null,
    liked = liked,
    likeCount = likeCount,
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
    var allPlaces: () -> List<MapPlace> = { emptyList() },
) : MapDetailRepository {

    val calls = mutableListOf<String>()

    override suspend fun getMapDetail(mapId: Long): MapDetail {
        calls += "getMapDetail"
        delay(responseDelayMillis)
        return map()
    }

    override suspend fun getPlaces(mapId: Long): List<MapPlace> {
        calls += "getPlaces"
        delay(responseDelayMillis)
        return allPlaces()
    }

    /** 참여를 실패시켜야 하는 테스트가 있다. 비어 있으면 성공한다. */
    var joinFailure: Throwable? = null

    override suspend fun joinMap(mapId: Long) {
        calls += "joinMap"
        delay(responseDelayMillis)
        joinFailure?.let { throw it }
    }

    override suspend fun leaveMap(mapId: Long) {
        calls += "leaveMap"
        delay(responseDelayMillis)
    }

    override suspend fun deleteMap(mapId: Long) {
        calls += "deleteMap"
        delay(responseDelayMillis)
    }

    /** 하트 요청을 실패시켜야 하는 테스트가 있다. 비어 있으면 성공한다. */
    var likeFailure: Throwable? = null

    /** 서버가 확정해 돌려줄 하트 수. 화면이 미리 올린 값과 달라야 확정이 보인다. */
    var confirmedLikeCount: Int = 10

    override suspend fun setPlaceLiked(placeId: Long, liked: Boolean): PlaceLike {
        calls += if (liked) "likePlace" else "unlikePlace"
        delay(responseDelayMillis)
        likeFailure?.let { throw it }
        return PlaceLike(liked = liked, likeCount = confirmedLikeCount)
    }
}
