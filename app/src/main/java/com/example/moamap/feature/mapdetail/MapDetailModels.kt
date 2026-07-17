package com.example.moamap.feature.mapdetail

import androidx.compose.runtime.Immutable

internal enum class MapDetailTab(val label: String) {
    Places("장소"),
    Logs("로그"),
}

@Immutable
internal data class MapDetailUiState(
    val selectedTab: MapDetailTab = MapDetailTab.Places,
    val selectedCategory: String = "전체",
    val selectedPlaceId: Long? = null,
)

internal fun MapDetailUiState.selectTab(tab: MapDetailTab): MapDetailUiState =
    copy(selectedTab = tab)

internal fun MapDetailUiState.selectCategory(category: String): MapDetailUiState =
    copy(selectedCategory = category)

internal fun MapDetailUiState.selectPlace(placeId: Long): MapDetailUiState =
    copy(selectedPlaceId = placeId)

internal fun MapDetailUiState.closePlaceDetail(): MapDetailUiState =
    copy(selectedPlaceId = null)

@Immutable
internal data class PlaceUiModel(
    val id: Long,
    val name: String,
    val description: String,
    val category: String,
    val area: String,
    val address: String,
    val rating: Double,
    val reviewCount: Int,
    val favorite: Boolean,
)

@Immutable
internal data class PlaceReviewUiModel(
    val id: Long,
    val userName: String,
    val rating: Int,
    val message: String,
    val relativeTime: String,
)

internal val MapDetailCategories = listOf(
    "전체",
    "데이트",
    "식당",
    "카페",
    "놀거리",
)

internal val SamplePlaces = listOf(
    PlaceUiModel(
        id = 1L,
        name = "커피나무",
        description = "따뜻한 분위기에서 스페셜티 커피를 즐길 수 있는 카페",
        category = "카페",
        area = "성수",
        address = "서울 성동구 성수이로 12",
        rating = 4.8,
        reviewCount = 124,
        favorite = true,
    ),
    PlaceUiModel(
        id = 2L,
        name = "달빛정원",
        description = "제철 식재료로 만든 계절 요리를 선보이는 아늑한 식당",
        category = "식당",
        area = "한남",
        address = "서울 용산구 한남대로 21",
        rating = 4.6,
        reviewCount = 87,
        favorite = false,
    ),
    PlaceUiModel(
        id = 3L,
        name = "책향기",
        description = "책과 커피를 함께 즐기며 쉬어 갈 수 있는 조용한 공간",
        category = "데이트",
        area = "연남",
        address = "서울 마포구 동교로5길 8",
        rating = 4.7,
        reviewCount = 63,
        favorite = true,
    ),
    PlaceUiModel(
        id = 4L,
        name = "루프탑 야경 바",
        description = "서울의 야경과 시그니처 칵테일을 함께 즐기는 루프탑 바",
        category = "놀거리",
        area = "이태원",
        address = "서울 용산구 이태원로 30",
        rating = 4.5,
        reviewCount = 51,
        favorite = false,
    ),
)

internal val SamplePlaceReviews = listOf(
    PlaceReviewUiModel(
        id = 1L,
        userName = "민지",
        rating = 5,
        message = "분위기가 편안하고 커피 향이 정말 좋았어요.",
        relativeTime = "2시간 전",
    ),
    PlaceReviewUiModel(
        id = 2L,
        userName = "준호",
        rating = 4,
        message = "조용해서 대화하기 좋고 공간도 아늑해요.",
        relativeTime = "1일 전",
    ),
    PlaceReviewUiModel(
        id = 3L,
        userName = "서연",
        rating = 5,
        message = "직원분들이 친절하고 메뉴도 만족스러웠어요.",
        relativeTime = "3일 전",
    ),
    PlaceReviewUiModel(
        id = 4L,
        userName = "지우",
        rating = 4,
        message = "다음에는 친구들과 다시 방문하고 싶어요.",
        relativeTime = "1주일 전",
    ),
)

internal fun filterPlaces(
    places: List<PlaceUiModel>,
    category: String,
): List<PlaceUiModel> = if (category == "전체") {
    places
} else {
    places.filter { it.category == category }
}
