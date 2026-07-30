package com.example.moamap.feature.mapdetail.presentation.logs

/**
 * 로그 탭 목데이터.
 *
 * 서버에 활동 내역 API 가 아직 없어 화면만 먼저 만든다. **연동할 때 이 파일을 지운다.**
 * 화면 파일에 섞어 두지 않는 이유가 그것이다.
 */

/** 사진 로딩 경로까지 확인하려고 실제로 열리는 주소를 쓴다. */
private const val SAMPLE_PHOTO_URL =
    "https://images.unsplash.com/photo-1554118811-1e0d58224f24?w=600"

internal val SampleMapLogs = listOf(
    MapLogUiModel(
        id = 1L,
        type = MapLogType.PlaceRemoved,
        userName = "김도현",
        userImageUrl = null,
        roleTag = "방장",
        message = "‘성수 감자탕’ 을 지도에서 삭제했어요",
        imageUrl = null,
        timeAgo = "10분 전",
    ),
    MapLogUiModel(
        id = 2L,
        type = MapLogType.PlaceAdded,
        userName = "이서연",
        userImageUrl = null,
        roleTag = "관리자",
        message = "‘어니언 성수’ 를 추가했어요",
        imageUrl = null,
        timeAgo = "2시간 전",
    ),
    MapLogUiModel(
        id = 3L,
        type = MapLogType.PlaceAdded,
        userName = "박지훈",
        userImageUrl = null,
        roleTag = null,
        message = "창가 자리가 넓어서 노트북 하기 좋아요. 오후 3시쯤 가면 한산합니다.",
        imageUrl = SAMPLE_PHOTO_URL,
        timeAgo = "5시간 전",
    ),
    MapLogUiModel(
        id = 4L,
        type = MapLogType.RoleChanged,
        userName = "김도현",
        userImageUrl = null,
        roleTag = "방장",
        message = "이서연 님에게 관리자 권한을 주었어요",
        imageUrl = null,
        timeAgo = "1일 전",
    ),
    MapLogUiModel(
        id = 5L,
        type = MapLogType.PlaceAdded,
        userName = "최유진",
        userImageUrl = null,
        roleTag = null,
        message = "‘대림창고’ 를 추가했어요",
        imageUrl = null,
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
