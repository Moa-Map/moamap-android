package com.example.moamap.feature.officialmap.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AreaCongestionTest {

    private fun congestion(
        ageRates: Map<AgeGroup, Double> = emptyMap(),
        maleRate: Double? = null,
        femaleRate: Double? = null,
    ) = AreaCongestion(
        level = CongestionLevel.NORMAL,
        message = null,
        populationMin = null,
        populationMax = null,
        ageRates = ageRates,
        maleRate = maleRate,
        femaleRate = femaleRate,
    )

    @Test
    fun `비율이 가장 높은 연령대를 고른다`() {
        val result = congestion(
            ageRates = mapOf(
                AgeGroup.TWENTIES to 23.0,
                AgeGroup.THIRTIES to 31.4,
                AgeGroup.FORTIES to 18.0,
            )
        ).dominantAge

        assertEquals("30대", result?.label)
        assertEquals(31.4, result?.rate!!, 0.001)
    }

    @Test
    fun `연령대가 동률이면 더 어린 쪽을 고른다`() {
        val result = congestion(
            ageRates = mapOf(AgeGroup.THIRTIES to 25.0, AgeGroup.TWENTIES to 25.0)
        ).dominantAge

        assertEquals("20대", result?.label)
    }

    @Test
    fun `연령대 정보가 없으면 null이다`() {
        assertNull(congestion().dominantAge)
    }

    @Test
    fun `비율이 더 높은 성별을 고른다`() {
        val result = congestion(maleRate = 54.2, femaleRate = 45.8).dominantGender

        assertEquals("남성", result?.label)
        assertEquals(54.2, result?.rate!!, 0.001)
    }

    @Test
    fun `성별이 동률이면 여성을 고른다`() {
        assertEquals("여성", congestion(maleRate = 50.0, femaleRate = 50.0).dominantGender?.label)
    }

    @Test
    fun `성별이 한쪽만 있으면 있는 쪽을 고른다`() {
        assertEquals("남성", congestion(maleRate = 60.0).dominantGender?.label)
        assertEquals("여성", congestion(femaleRate = 60.0).dominantGender?.label)
    }

    @Test
    fun `성별 정보가 없으면 null이다`() {
        assertNull(congestion().dominantGender)
    }
}
