package com.moamap.app.feature.mapdetail.domain.model

/**
 * 카카오 카테고리 그룹. 마커에 넣을 아이콘을 고르는 기준이다.
 *
 * 서버는 카카오의 `category_group_code` 를 저장하지 않고 분류 경로(`category_name`)만
 * 내려준다. 그래서 코드 대신 경로 앞부분으로 그룹을 되짚는다 - [fromCategoryPath] 참고.
 */
enum class PlaceCategoryGroup {
    Mart,
    ConvenienceStore,
    Childcare,
    School,
    Academy,
    Parking,
    GasStation,
    Subway,
    Bank,
    Culture,
    RealEstate,
    PublicOffice,
    Attraction,
    Lodging,
    Restaurant,
    Cafe,
    Hospital,
    Pharmacy,
    ;

    companion object {
        /**
         * 분류 경로 앞부분 → 그룹. 위에서부터 처음 맞는 줄을 쓴다.
         *
         * 카카오 로컬 API 에서 그룹 코드별로 받은 장소의 경로를 보고 정했다. 순서가 곧 규칙이다 -
         * 카페는 경로가 `음식점 > 카페` 로 시작하므로 음식점보다 먼저 봐야 한다. 주차장과
         * 주유소는 둘째 토막(`교통시설`, `자동차`)이 다른 시설과 겹쳐 셋째 토막까지 본다.
         */
        private val PathRules: List<Pair<List<String>, PlaceCategoryGroup>> = listOf(
            listOf("음식점", "카페") to Cafe,
            listOf("음식점") to Restaurant,
            listOf("가정,생활", "대형마트") to Mart,
            listOf("가정,생활", "슈퍼마켓") to Mart,
            listOf("가정,생활", "편의점") to ConvenienceStore,
            listOf("교육,학문", "유아교육") to Childcare,
            listOf("교육,학문", "학교") to School,
            listOf("교육,학문", "학원") to Academy,
            listOf("교통,수송", "교통시설", "주차장") to Parking,
            listOf("교통,수송", "자동차", "주유,가스") to GasStation,
            listOf("교통,수송", "지하철,전철") to Subway,
            listOf("금융,보험", "금융서비스") to Bank,
            listOf("문화,예술", "문화시설") to Culture,
            listOf("문화,예술", "영화,영상") to Culture,
            listOf("부동산", "부동산서비스") to RealEstate,
            listOf("사회,공공기관") to PublicOffice,
            listOf("여행", "관광,명소") to Attraction,
            listOf("여행", "숙박") to Lodging,
            listOf("의료,건강", "병원") to Hospital,
            listOf("의료,건강", "약국") to Pharmacy,
        )

        /**
         * `"음식점 > 카페 > 커피전문점"` 같은 경로가 속한 그룹.
         *
         * 어느 그룹에도 들지 않거나(쇼핑·스포츠 등) 경로가 비어 있으면 null. 마커는 그때
         * 기본 그림을 쓴다.
         */
        fun fromCategoryPath(path: String): PlaceCategoryGroup? {
            val segments = path.split(">").map { it.trim() }
            return PathRules.firstOrNull { (prefix, _) ->
                segments.size >= prefix.size && segments.subList(0, prefix.size) == prefix
            }?.second
        }
    }
}
