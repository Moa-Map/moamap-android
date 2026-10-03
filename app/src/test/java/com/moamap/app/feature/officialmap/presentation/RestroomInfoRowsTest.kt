package com.moamap.app.feature.officialmap.presentation

import com.moamap.app.feature.officialmap.domain.model.RestroomDetail
import org.junit.Assert.assertEquals
import org.junit.Test

class RestroomInfoRowsTest {

    private fun detail(
        address: String? = null,
        openHours: String? = null,
        openHoursDetail: String? = null,
        maleToilet: Int = 0,
        maleUrinal: Int = 0,
        maleDisabledToilet: Int = 0,
        femaleToilet: Int = 0,
        femaleDisabledToilet: Int = 0,
        diaperTable: Boolean = false,
        emergencyBell: Boolean = false,
        entranceCctv: Boolean = false,
        managerOrg: String? = null,
        phone: String? = null,
    ) = RestroomDetail(
        id = 1, name = "화장실", category = null, address = address,
        openHours = openHours, openHoursDetail = openHoursDetail,
        maleToilet = maleToilet, maleUrinal = maleUrinal,
        maleDisabledToilet = maleDisabledToilet, maleDisabledUrinal = 0,
        maleChildToilet = 0, maleChildUrinal = 0,
        femaleToilet = femaleToilet, femaleDisabledToilet = femaleDisabledToilet, femaleChildToilet = 0,
        diaperTable = diaperTable, emergencyBell = emergencyBell, entranceCctv = entranceCctv,
        managerOrg = managerOrg, phone = phone, dataRefDate = null,
    )

    @Test
    fun `시청역 화장실처럼 다 있으면 항목 순서대로 줄을 만든다`() {
        val rows = detail(
            address = "서울특별시 중구 세종대로 지하 101",
            openHours = "정시",
            openHoursDetail = "05:00~24:00",
            maleToilet = 6,
            maleUrinal = 5,
            maleDisabledToilet = 1,
            femaleToilet = 15,
            femaleDisabledToilet = 1,
            diaperTable = true,
            emergencyBell = true,
            entranceCctv = true,
            managerOrg = "서울교통공사",
            phone = "02-6110-1321",
        ).toInfoRows()

        assertEquals(
            listOf(
                "주소" to "서울특별시 중구 세종대로 지하 101",
                "개방시간" to "정시 · 05:00~24:00",
                "남자" to "대변기 6 · 소변기 5",
                "여자" to "대변기 15",
                "장애인용" to "남 대변기 1 · 여 대변기 1",
                "편의시설" to "기저귀 교환대 · 비상벨 · 입구 CCTV",
                "관리기관" to "서울교통공사 · 02-6110-1321",
            ),
            rows,
        )
    }

    @Test
    fun `0인 칸과 없는 시설은 빼고, 남는 게 없는 항목은 줄째 뺀다`() {
        val rows = detail(maleUrinal = 2, phone = "02-000-0000").toInfoRows()

        assertEquals(listOf("남자" to "소변기 2", "관리기관" to "02-000-0000"), rows)
    }
}
