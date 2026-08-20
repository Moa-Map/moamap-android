package com.moamap.app.feature.officialmap.presentation

import com.moamap.app.feature.officialmap.domain.model.AgeGroup
import com.moamap.app.feature.officialmap.domain.model.AreaCongestion
import com.moamap.app.feature.officialmap.domain.model.CongestionLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AreaStatsTest {

    private fun congestion(
        populationMin: Long? = null,
        ageRates: Map<AgeGroup, Double> = emptyMap(),
        maleRate: Double? = null,
        femaleRate: Double? = null,
    ) = AreaCongestion(
        level = CongestionLevel.NORMAL,
        message = null,
        populationMin = populationMin,
        populationMax = null,
        ageRates = ageRates,
        maleRate = maleRate,
        femaleRate = femaleRate,
    )

    @Test
    fun `인구와 최다 연령대 성별을 세 칸으로 만든다`() {
        val stats = congestion(
            populationMin = 50_000,
            ageRates = mapOf(AgeGroup.THIRTIES to 31.4, AgeGroup.TWENTIES to 23.0),
            maleRate = 54.2,
            femaleRate = 45.8,
        ).toAreaStats()

        assertEquals(
            listOf(
                AreaStat("인구", "약 5만 명"),
                AreaStat("30대 비율", "31%"),
                AreaStat("남성 비율", "54%"),
            ),
            stats,
        )
    }

    @Test
    fun `1만 명 미만은 만 단위로 뭉개지 않는다`() {
        // 실제 API 121개 지역 중 79개가 1만 미만이고 최소는 100명이다.
        assertEquals("약 9,500명", congestion(populationMin = 9_500).toAreaStats().single().value)
        assertEquals("약 100명", congestion(populationMin = 100).toAreaStats().single().value)
    }

    @Test
    fun `1만 명 이상은 만 단위로 줄이되 자투리를 남긴다`() {
        assertEquals("약 1.6만 명", congestion(populationMin = 16_000).toAreaStats().single().value)
        assertEquals("약 12만 명", congestion(populationMin = 120_000).toAreaStats().single().value)
    }

    @Test
    fun `값이 없는 칸은 빠진다`() {
        val stats = congestion(femaleRate = 56.0).toAreaStats()

        assertEquals(listOf(AreaStat("여성 비율", "56%")), stats)
    }

    @Test
    fun `혼잡도 정보가 없으면 빈 목록이다`() {
        assertTrue(null.toAreaStats().isEmpty())
    }
}
