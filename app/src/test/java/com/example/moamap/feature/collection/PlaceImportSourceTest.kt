package com.example.moamap.feature.collection

import com.example.moamap.core.navigation.MoaMapRoute
import com.example.moamap.feature.collection.domain.model.PlaceImportSource
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaceImportSourceTest {

    @Test
    fun `링크 없이 들어오는 흐름만 URL 입력을 건너뛴다`() {
        // 이 플래그가 틀리면 워치에서 들어온 사용자가 빈 URL 입력 화면에 갇힌다.
        assertTrue(PlaceImportSource.WalkPoint.skipsUrlInput)
        assertTrue(PlaceImportSource.WalkRecordSingle.skipsUrlInput)
        assertTrue(PlaceImportSource.WalkRecordMulti.skipsUrlInput)
        assertFalse(PlaceImportSource.Instagram.skipsUrlInput)
        assertFalse(PlaceImportSource.MapShare.skipsUrlInput)
    }

    // 아래 두 개는 경로 전체를 견주지 않는다. `Uri.encode` 가 단위 테스트에서는 스텁이라
    // 빈 문자열 대신 null 을 돌려주기 때문이다(실기기에서는 "" 다). 이번에 더한 좌표
    // 부분만 확인해 그 스텁에 얽매이지 않게 한다.

    @Test
    fun `좌표를 넘기면 경로 질의에 실린다`() {
        val route = MoaMapRoute.PlaceImport.createRoute(
            source = PlaceImportSource.WalkPoint.name,
            lat = 37.4963,
            lng = 126.9574,
        )

        assertTrue(route.startsWith("place_import/WalkPoint?"))
        assertTrue(route.endsWith("&lat=37.4963&lng=126.9574"))
    }

    @Test
    fun `좌표를 넘기지 않으면 빈 값으로 남는다`() {
        // 기존 두 카드는 좌표 없이 들어온다. 경로가 달라지면 이동 자체가 실패한다.
        val route = MoaMapRoute.PlaceImport.createRoute(PlaceImportSource.Instagram.name)

        assertTrue(route.startsWith("place_import/Instagram?"))
        assertTrue(route.endsWith("&lat=&lng="))
    }
}
