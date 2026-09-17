package com.moamap.app.feature.mapdetail.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * 분류 경로 → 카테고리 그룹.
 *
 * 경로 예시는 카카오 로컬 API 카테고리 검색(그룹 코드별)에서 실제로 받은 값이다.
 */
class PlaceCategoryGroupTest {

    private fun group(path: String) = PlaceCategoryGroup.fromCategoryPath(path)

    @Test
    fun `그룹 코드마다 실제 경로가 해당 그룹으로 간다`() {
        val cases = mapOf(
            "가정,생활 > 대형마트 > 롯데마트" to PlaceCategoryGroup.Mart,
            "가정,생활 > 슈퍼마켓 > 대형슈퍼" to PlaceCategoryGroup.Mart,
            "가정,생활 > 편의점 > GS25" to PlaceCategoryGroup.ConvenienceStore,
            "교육,학문 > 유아교육 > 어린이집" to PlaceCategoryGroup.Childcare,
            "교육,학문 > 학교 > 대학교" to PlaceCategoryGroup.School,
            "교육,학문 > 학원 > 외국어학원" to PlaceCategoryGroup.Academy,
            "교통,수송 > 교통시설 > 주차장" to PlaceCategoryGroup.Parking,
            "교통,수송 > 자동차 > 주유,가스 > SK에너지" to PlaceCategoryGroup.GasStation,
            "교통,수송 > 지하철,전철 > 수도권7호선" to PlaceCategoryGroup.Subway,
            "금융,보험 > 금융서비스 > 은행 > 신한은행" to PlaceCategoryGroup.Bank,
            "문화,예술 > 문화시설 > 미술관" to PlaceCategoryGroup.Culture,
            "문화,예술 > 영화,영상 > 영화관" to PlaceCategoryGroup.Culture,
            "부동산 > 부동산서비스 > 부동산중개" to PlaceCategoryGroup.RealEstate,
            "사회,공공기관 > 행정기관 > 주민센터" to PlaceCategoryGroup.PublicOffice,
            "사회,공공기관 > 외국기관 > 대사관" to PlaceCategoryGroup.PublicOffice,
            "여행 > 관광,명소 > 고궁" to PlaceCategoryGroup.Attraction,
            "여행 > 숙박 > 호텔" to PlaceCategoryGroup.Lodging,
            "음식점 > 한식 > 국밥" to PlaceCategoryGroup.Restaurant,
            "음식점 > 카페 > 커피전문점" to PlaceCategoryGroup.Cafe,
            "의료,건강 > 병원 > 치과" to PlaceCategoryGroup.Hospital,
            "의료,건강 > 약국" to PlaceCategoryGroup.Pharmacy,
        )

        cases.forEach { (path, expected) -> assertEquals(path, expected, group(path)) }
    }

    @Test
    fun `카페는 음식점보다 먼저 가린다`() {
        assertEquals(PlaceCategoryGroup.Cafe, group("음식점 > 카페"))
        assertEquals(PlaceCategoryGroup.Restaurant, group("음식점 > 카페테리아"))
    }

    @Test
    fun `주차장·주유소는 셋째 토막까지 맞아야 한다`() {
        assertNull(group("교통,수송 > 교통시설 > 교량"))
        assertNull(group("교통,수송 > 자동차 > 자동차정비"))
        assertNull(group("교통,수송 > 교통시설"))
    }

    @Test
    fun `토막 앞뒤 공백이 달라도 같은 경로로 본다`() {
        assertEquals(PlaceCategoryGroup.Cafe, group("음식점>카페>커피전문점"))
        assertEquals(PlaceCategoryGroup.Pharmacy, group("  의료,건강 >  약국 "))
    }

    @Test
    fun `어느 그룹에도 들지 않으면 null`() {
        assertNull(group(""))
        assertNull(group("쇼핑,유통 > 의류판매"))
        assertNull(group("스포츠,레저 > 볼링장"))
        assertNull(group("가정,생활 > 생활용품점"))
        // 토막 일부만 같은 이름은 다른 분류다.
        assertNull(group("음식점점 > 한식"))
    }
}
