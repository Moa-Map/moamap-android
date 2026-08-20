package com.moamap.app.feature.officialmap.data.repository

import com.moamap.app.feature.officialmap.data.remote.CongestionDto
import com.moamap.app.feature.officialmap.data.remote.FootTrafficAreaDto
import com.moamap.app.feature.officialmap.data.remote.FootTrafficService
import com.moamap.app.feature.officialmap.domain.model.CongestionLevel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

private class FakeFootTrafficService(
    private val areas: List<FootTrafficAreaDto>,
    private val congestions: List<CongestionDto>,
) : FootTrafficService {
    override suspend fun getAreas(): List<FootTrafficAreaDto> = areas
    override suspend fun getCongestions(): List<CongestionDto> = congestions
}

class FootTrafficRepositoryImplTest {

    private fun area(code: String, name: String = "지역$code") = FootTrafficAreaDto(
        footTrafficAreaCd = code, areaNm = name, lat = 37.5, lng = 126.9,
    )

    private fun congestion(code: String, level: String) = CongestionDto(
        footTrafficAreaCd = code, congestLvl = level,
    )

    @Test
    fun `지역과 혼잡도를 코드로 조인한다`() = runTest {
        val repository = FootTrafficRepositoryImpl(
            FakeFootTrafficService(
                areas = listOf(area("A"), area("B")),
                congestions = listOf(congestion("B", "붐빔"), congestion("A", "여유")),
            )
        )

        val result = repository.getDensityAreas()

        assertEquals(2, result.size)
        assertEquals(CongestionLevel.RELAXED, result.first { it.code == "A" }.congestion?.level)
        assertEquals(CongestionLevel.BUSY, result.first { it.code == "B" }.congestion?.level)
    }

    @Test
    fun `혼잡도 없는 지역도 목록에 남고 congestion은 null이다`() = runTest {
        val repository = FootTrafficRepositoryImpl(
            FakeFootTrafficService(
                areas = listOf(area("A"), area("C")),
                congestions = listOf(congestion("A", "보통")),
            )
        )

        val result = repository.getDensityAreas()

        assertEquals(2, result.size)
        assertNull(result.first { it.code == "C" }.congestion)
    }
}
