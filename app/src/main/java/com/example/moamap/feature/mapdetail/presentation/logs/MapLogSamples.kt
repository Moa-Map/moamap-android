package com.example.moamap.feature.mapdetail.presentation.logs

/**
 * 프리뷰 전용 표본.
 *
 * 활동 내역은 서버에서 받아 오지만 프리뷰는 네트워크를 타지 않는다. 화면 파일마다 표본을
 * 흩어 두지 않으려고 여기 모은다.
 *
 * **화면에 넘기지 않는다.**
 * 여기 값은 프리뷰에만 쓴다.
 */

internal val SampleMapLogs = listOf(
    MapLogUiModel(
        id = "0",
        type = MapLogType.PlaceRemoved,
        userName = "김도현",
        userImageUrl = null,
        message = "‘성수 감자탕’ 을 지도에서 삭제했어요",
        timeAgo = "10분 전",
    ),
    MapLogUiModel(
        id = "1",
        type = MapLogType.PlaceAdded,
        userName = "이서연",
        userImageUrl = null,
        message = "‘어니언 성수’ 를 추가했어요",
        timeAgo = "2시간 전",
    ),
    MapLogUiModel(
        id = "2",
        type = MapLogType.ReviewCreated,
        userName = "박지훈",
        userImageUrl = null,
        message = "‘대림창고’ 에 별점 4점 후기를 남겼어요",
        timeAgo = "5시간 전",
    ),
    MapLogUiModel(
        id = "3",
        type = MapLogType.PlaceAdded,
        userName = "최유진",
        userImageUrl = null,
        message = "‘성수 브루어리’ 를 추가했어요",
        timeAgo = "2일 전",
    ),
)

internal val SamplePendingRequests = listOf(
    PendingRequestUiModel(
        id = 101L,
        userName = "박지훈",
        userImageUrl = null,
        message = "‘성수 브루어리’ 를 이 지도에 추가하고 싶어요",
        timeAgo = "30분 전",
    ),
)
