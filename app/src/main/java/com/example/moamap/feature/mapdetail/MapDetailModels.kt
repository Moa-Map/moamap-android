package com.example.moamap.feature.mapdetail

import androidx.compose.runtime.Immutable
import com.example.moamap.feature.mapdetail.domain.model.MapPlace
import com.example.moamap.feature.mapdetail.domain.model.PlaceReview
import com.example.moamap.feature.mapdetail.domain.model.areaLabel
import com.example.moamap.feature.mapdetail.domain.model.categoryLabel

internal enum class MapDetailTab(val label: String) {
    Places("장소"),
    Logs("로그"),
}

@Immutable
internal data class MapDetailUiState(
    val selectedTab: MapDetailTab = MapDetailTab.Places,
    val selectedPlaceId: Long? = null,
    /** 바텀시트 검색어. 받아 둔 목록을 이 자리에서 거른다. */
    val searchQuery: String = "",
)

internal fun MapDetailUiState.selectTab(tab: MapDetailTab): MapDetailUiState =
    copy(selectedTab = tab)

internal fun MapDetailUiState.selectPlace(placeId: Long): MapDetailUiState =
    copy(selectedPlaceId = placeId)

internal fun MapDetailUiState.closePlaceDetail(): MapDetailUiState =
    copy(selectedPlaceId = null)

internal fun MapDetailUiState.search(query: String): MapDetailUiState =
    copy(searchQuery = query)

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
    /** 목록 썸네일. 없으면 플레이스홀더를 띄운다. */
    val photoUrl: String? = null,
)

/**
 * 서버에서 받은 장소를 목록 카드가 읽는 모양으로 옮긴다.
 *
 * [PlaceUiModel.favorite] 은 늘 false 다. 즐겨찾기에 해당하는 서버 필드가 아직 없어
 * 켤 근거가 없다. 하트는 디자인대로 그려지되 빈 상태로 남는다.
 */
internal fun MapPlace.toPlaceUiModel(): PlaceUiModel = PlaceUiModel(
    id = id,
    name = name,
    description = description,
    category = categoryLabel,
    area = areaLabel,
    address = address,
    rating = rating,
    reviewCount = reviewCount,
    favorite = false,
    photoUrl = photoUrl,
)

/**
 * 이름으로 장소를 거른다. 검색어가 비면 전부 남긴다.
 *
 * 이미 받아 둔 목록을 그 자리에서 거른다. 조회 API 에 검색 파라미터가 없기도 하고,
 * 어차피 전량을 들고 있어 서버를 한 번 더 다녀올 이유가 없다.
 *
 * 주소는 보지 않는다. `"서울"` 같은 걸 치면 지도 전체가 그대로 남아 거른 티가 안 난다.
 */
internal fun searchPlaces(
    places: List<PlaceUiModel>,
    query: String,
): List<PlaceUiModel> {
    val keyword = query.trim()
    if (keyword.isEmpty()) return places

    return places.filter { place -> place.name.contains(keyword, ignoreCase = true) }
}

@Immutable
internal data class PlaceReviewUiModel(
    val id: Long,
    val userName: String,
    val rating: Int,
    val message: String,
    val relativeTime: String,
)

/**
 * 장소 상세 시트의 후기 영역 상태.
 * 목록·조회 실패·작성 진행을 한 덩어리로 넘긴다.
 */
@Immutable
internal data class PlaceReviewsUiModel(
    val loading: Boolean = false,
    val items: List<PlaceReviewUiModel> = emptyList(),
    val loadErrorMessage: String? = null,
    val submitting: Boolean = false,
    /** 시트 위에는 스낵바를 띄울 수 없어 입력창 아래에 남긴다. */
    val submitErrorMessage: String? = null,
    /** 서버가 받아들인 후기 수. 늘어나면 입력창을 비운다. */
    val submittedCount: Int = 0,
)

/** 닉네임을 못 얻은 작성자. 이름 자리가 빈 줄로 보이지 않게 채운다. */
private const val ANONYMOUS_REVIEWER = "이름 없는 사용자"

internal fun PlaceReview.toPlaceReviewUiModel(nowMillis: Long): PlaceReviewUiModel =
    PlaceReviewUiModel(
        id = id,
        userName = authorName ?: ANONYMOUS_REVIEWER,
        rating = rating,
        message = content,
        relativeTime = relativeTimeLabel(createdAtMillis, nowMillis),
    )

private const val MINUTE_MILLIS = 60_000L
private const val HOUR_MILLIS = 60 * MINUTE_MILLIS
private const val DAY_MILLIS = 24 * HOUR_MILLIS
private const val WEEK_MILLIS = 7 * DAY_MILLIS
private const val MONTH_MILLIS = 30 * DAY_MILLIS
private const val YEAR_MILLIS = 365 * DAY_MILLIS

/**
 * 시각을 못 읽었으면 빈 문자열이고, 화면은 그 자리를 비운다.
 */
internal fun relativeTimeLabel(createdAtMillis: Long?, nowMillis: Long): String {
    if (createdAtMillis == null) return ""

    val elapsed = nowMillis - createdAtMillis
    return when {
        elapsed < MINUTE_MILLIS -> "방금 전"
        elapsed < HOUR_MILLIS -> "${elapsed / MINUTE_MILLIS}분 전"
        elapsed < DAY_MILLIS -> "${elapsed / HOUR_MILLIS}시간 전"
        elapsed < WEEK_MILLIS -> "${elapsed / DAY_MILLIS}일 전"
        elapsed < MONTH_MILLIS -> "${elapsed / WEEK_MILLIS}주일 전"
        elapsed < YEAR_MILLIS -> "${elapsed / MONTH_MILLIS}개월 전"
        else -> "${elapsed / YEAR_MILLIS}년 전"
    }
}

/** 미리보기 전용 장소. 프리뷰는 네트워크를 타지 않아 썸네일 자리는 플레이스홀더로 뜬다. */
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
