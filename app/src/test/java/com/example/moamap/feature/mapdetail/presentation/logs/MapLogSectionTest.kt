package com.example.moamap.feature.mapdetail.presentation.logs

import com.example.moamap.feature.collection.domain.model.MapType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MapLogSectionTest {

    private fun log(id: Long, type: MapLogType, roleTag: String? = "방장") = MapLogUiModel(
        id = id,
        type = type,
        userName = "김도현",
        userImageUrl = null,
        roleTag = roleTag,
        message = "메시지",
        imageUrl = null,
        timeAgo = "1시간 전",
    )

    private val logs = listOf(
        log(1L, MapLogType.PlaceAdded),
        log(2L, MapLogType.RoleChanged),
        log(3L, MapLogType.PlaceRemoved),
        log(4L, MapLogType.RoleChanged),
    )

    /** 프라이빗 지도에는 권한 개념이 없다. 서버가 실수로 내려줘도 화면에 흘리지 않는다. */
    @Test
    fun `프라이빗 지도는 권한 변경 로그를 걸러낸다`() {
        val filtered = logs.forMapType(MapType.Private)

        assertEquals(listOf(1L, 3L), filtered.map { it.id })
    }

    /**
     * 종류만 걸러서는 부족하다. 남은 장소 로그에 "방장" 태그가 그대로 붙어 있으면
     * 프라이빗 지도에 역할이 있는 것처럼 보인다.
     */
    @Test
    fun `프라이빗 지도는 역할 태그도 떼어낸다`() {
        val filtered = logs.forMapType(MapType.Private)

        assertTrue(filtered.isNotEmpty())
        assertTrue(filtered.all { it.roleTag == null })
    }

    @Test
    fun `공개 지도는 역할 태그를 그대로 둔다`() {
        assertEquals(logs, logs.forMapType(MapType.Community))
    }

    @Test
    fun `걸러낸 뒤에도 순서를 유지한다`() {
        val filtered = listOf(
            log(3L, MapLogType.PlaceRemoved),
            log(2L, MapLogType.RoleChanged),
            log(1L, MapLogType.PlaceAdded),
        ).forMapType(MapType.Private)

        assertEquals(listOf(3L, 1L), filtered.map { it.id })
    }

    @Test
    fun `권한 변경뿐이면 프라이빗에서는 빈 목록이 된다`() {
        val filtered = listOf(log(1L, MapLogType.RoleChanged)).forMapType(MapType.Private)

        assertTrue(filtered.isEmpty())
    }

    @Test
    fun `빈 목록은 그대로 빈 목록이다`() {
        assertTrue(emptyList<MapLogUiModel>().forMapType(MapType.Private).isEmpty())
    }
}
