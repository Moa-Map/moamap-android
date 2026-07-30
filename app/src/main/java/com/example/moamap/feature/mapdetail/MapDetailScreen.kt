package com.example.moamap.feature.mapdetail

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import com.example.moamap.R
import com.example.moamap.feature.mapdetail.presentation.logs.MapActivityViewModel
import com.example.moamap.feature.mapdetail.presentation.logs.MapLogUiModel
import com.example.moamap.feature.mapdetail.presentation.logs.MapLogsContent
import com.example.moamap.feature.mapdetail.presentation.logs.PendingRequestUiModel
import com.example.moamap.feature.mapdetail.presentation.logs.SampleMapLogs
import com.example.moamap.feature.mapdetail.presentation.logs.SamplePendingRequests
import com.example.moamap.feature.mapdetail.presentation.logs.toMapLogUiModels
import com.example.moamap.feature.mapdetail.presentation.members.MemberSheet
import com.example.moamap.feature.mapdetail.presentation.members.SampleMembers
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.moamap.core.designsystem.component.ErrorSnackbar
import com.example.moamap.core.designsystem.theme.MoaMapPrimitiveColors
import com.example.moamap.core.designsystem.theme.MoaMapTheme
import com.example.moamap.feature.mapdetail.domain.model.MapDetailAction
import com.example.moamap.feature.mapdetail.presentation.MapDetailViewModel
import com.example.moamap.feature.mapdetail.presentation.addplace.AddPlaceSheet
import com.example.moamap.feature.mapdetail.presentation.addplace.AddPlaceViewModel
import com.example.moamap.feature.mapdetail.presentation.MapLoadState
import com.example.moamap.feature.mapdetail.presentation.mapOrNull
import com.example.moamap.feature.mapdetail.presentation.review.PlaceReviewViewModel
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.dsl.cameraOptions
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import kotlinx.coroutines.launch

/** 시트가 가리지 않도록 지도 컨트롤을 시트 위로 띄우는 여백. */
private val MapControlsBottomGap = 16.dp

/** 접힌 시트 높이. 제목·검색창까지만 보이고 장소 카드는 올려야 나온다. */
private val SheetPeekHeight = 187.dp

private val LocationPermissions = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION,
)

private fun hasLocationPermission(context: Context): Boolean =
    LocationPermissions.any { permission ->
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

private val MapDetailUiStateSaver = listSaver<MapDetailUiState, String>(
    save = { state ->
        listOf(
            state.selectedTab.name,
            state.selectedPlaceId?.toString().orEmpty(),
            state.searchQuery,
        )
    },
    restore = { values ->
        MapDetailUiState(
            selectedTab = values.getOrNull(0)?.let { savedTabName ->
                MapDetailTab.entries.firstOrNull { tab -> tab.name == savedTabName }
            } ?: MapDetailTab.Places,
            selectedPlaceId = values.getOrNull(1)?.toLongOrNull(),
            searchQuery = values.getOrNull(2).orEmpty(),
        )
    },
)

@Composable
fun MapDetailScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** 서버 응답이 오기 전 상단바를 채우는 초기값. 응답이 도착하면 덮어쓴다. */
    initialTitle: String = "",
    viewModel: MapDetailViewModel = hiltViewModel(),
    addPlaceViewModel: AddPlaceViewModel = hiltViewModel(),
    reviewViewModel: PlaceReviewViewModel = hiltViewModel(),
    activityViewModel: MapActivityViewModel = hiltViewModel(),
) {
    val screenState by viewModel.uiState.collectAsStateWithLifecycle()
    val reviewState by reviewViewModel.uiState.collectAsStateWithLifecycle()
    val activityState by activityViewModel.uiState.collectAsStateWithLifecycle()

    // 나가기가 끝나면 왔던 곳(탐색 또는 모음)으로 돌아간다.
    LaunchedEffect(screenState.left) {
        if (screenState.left) onBackClick()
    }

    val context = LocalContext.current
    val granted = hasLocationPermission(context)

    var locationGranted by remember { mutableStateOf(granted) }

    /**
     * 권한 요청에 답이 왔는지.
     * 답을 기다리지 않고 카메라를 정하면, 장소가 없는 지도에서 현재 위치를 놓치고
     */
    var permissionAnswered by remember { mutableStateOf(granted) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        locationGranted = result.values.any { isGranted -> isGranted }
        permissionAnswered = true
    }

    // 들어올 때마다 묻는다. 안드로이드가 두 번 거절 이후로는 다이얼로그 없이 즉시
    // 거절하므로, 그때부터는 콜백만 돌아오고 조용히 폴백으로 넘어간다.
    LaunchedEffect(Unit) {
        if (!locationGranted) permissionLauncher.launch(LocationPermissions)
    }

    // 등록 완료 안내. 시트가 닫힌 뒤 상세 화면에서 띄운다.
    var addPlaceNotice by remember { mutableStateOf<String?>(null) }
    var addPlaceSheetVisible by rememberSaveable { mutableStateOf(false) }
    var memberSheetVisible by rememberSaveable { mutableStateOf(false) }

    var uiState by rememberSaveable(stateSaver = MapDetailUiStateSaver) {
        mutableStateOf(MapDetailUiState())
    }
    val places = remember(screenState.places) {
        screenState.places.map { place -> place.toPlaceUiModel() }
    }
    val visiblePlaces = remember(places, uiState.searchQuery) {
        searchPlaces(places, uiState.searchQuery)
    }
    // 검색으로 목록에서 빠진 장소라도, 마커로 눌러 열어 둔 상세는 닫히면 안 된다.
    val selectedPlace = places.firstOrNull { place ->
        place.id == uiState.selectedPlaceId
    }
    val closePlaceDetail = { uiState = uiState.closePlaceDetail() }

    // 시트를 연 장소의 후기를 읽는다. 닫으면 비워, 다음에 열 때 서버에서 다시 읽는다.
    LaunchedEffect(uiState.selectedPlaceId) {
        val placeId = uiState.selectedPlaceId
        if (placeId == null) reviewViewModel.close() else reviewViewModel.open(placeId)
    }

    // 로그 탭을 처음 열 때 활동 내역을 읽는다. 장소 탭만 보고 나가면 조회가 아예 안 나간다.
    LaunchedEffect(uiState.selectedTab) {
        if (uiState.selectedTab == MapDetailTab.Logs) activityViewModel.loadOnce()
    }

    // 후기가 하나 늘면 장소의 평점·후기 수도 달라진다. 시트 뒤의 목록이 옛 값을 들고 있으면 안 된다.
    LaunchedEffect(reviewState.submittedCount) {
        if (reviewState.submittedCount > 0) viewModel.refresh()
    }

    // 시트가 읽는 후기 상태. 조회는 시트가 그려진 뒤에 시작하므로, 상태가 어느 장소의
    // 것인지 확인하지 않으면 직전 장소의 후기가 한 프레임 스쳐 간다.
    val reviews = remember(reviewState, uiState.selectedPlaceId) {
        if (reviewState.placeId != uiState.selectedPlaceId) {
            PlaceReviewsUiModel(loading = true)
        } else {
            val now = System.currentTimeMillis()
            PlaceReviewsUiModel(
                loading = reviewState.loading,
                items = reviewState.reviews.map { review -> review.toPlaceReviewUiModel(now) },
                loadErrorMessage = reviewState.loadErrorMessage,
                submitting = reviewState.submitting,
                submitErrorMessage = reviewState.submitErrorMessage,
                submittedCount = reviewState.submittedCount,
            )
        }
    }

    // "2시간 전" 은 그리는 시점을 기준으로 한다. 목록이 바뀔 때만 다시 계산해, 재구성마다
    // 시각을 새로 읽어 같은 값을 두고 목록 전체가 갈리는 일을 막는다.
    val logs = remember(activityState.activities) {
        activityState.activities.toMapLogUiModels(System.currentTimeMillis())
    }

    val markers = remember(screenState.places) {
        screenState.places.map { place -> place.toPlaceMarker() }
    }
    val mapViewportState = rememberMapViewportState {
        setCameraOptions {
            center(MapDetailCenter)
            zoom(MapDetailDefaultZoom)
        }
    }

    /**
     * 초기 카메라를 한 번만 맞춘다.
     * 잠그지 않으면 그때마다 사용자가 옮겨 둔 카메라가 튄다.
     */
    var cameraSettled by rememberSaveable { mutableStateOf(false) }

    /** 첫 조회가 끝났는지. 지도와 장소는 같은 갱신으로 함께 들어온다. */
    val loadFinished = screenState.map !is MapLoadState.Loading

    /**
     * 마커를 화면에 맞출 때 가장자리에 두는 여백.
     */
    val fitPadding = with(LocalDensity.current) {
        EdgeInsets(
            80.dp.toPx().toDouble(),
            40.dp.toPx().toDouble(),
            (SheetPeekHeight + 40.dp).toPx().toDouble(),
            40.dp.toPx().toDouble(),
        )
    }

    LaunchedEffect(loadFinished, permissionAnswered, cameraSettled) {
        if (cameraSettled || !loadFinished) return@LaunchedEffect

        val places = screenState.places
        // 장소가 있으면 위치를 기다릴 이유가 없다. 없을 때만 권한 답을 기다린다.
        if (places.isEmpty() && !permissionAnswered) return@LaunchedEffect

        val location = if (places.isEmpty() && locationGranted) lastKnownLocation() else null

        when (val camera = initialCamera(places, location)) {
            // cameraForCoordinates 는 지도가 붙은 뒤에야 답한다. 로그 탭을 복원한 채로
            // 들어오면 장소 탭으로 옮길 때까지 여기서 매달린다 - 화면은 폴백 좌표로 떠 있다.
            is InitialCamera.Fit -> mapViewportState.setCameraOptions(
                mapViewportState.cameraForCoordinates(
                    coordinates = camera.points,
                    coordinatesPadding = fitPadding,
                    maxZoom = MapDetailDefaultZoom,
                ),
            )
            is InitialCamera.Center -> mapViewportState.setCameraOptions {
                center(camera.point)
                zoom(MapDetailDefaultZoom)
            }
        }
        cameraSettled = true
    }
    val onMarkerClick: (Long) -> Unit = remember {
        { placeId -> uiState = uiState.selectPlace(placeId) }
    }
    val onClusterClick: (MarkerCluster) -> Unit = remember(mapViewportState) {
        { cluster ->
            // 중심과 줌만 건드린다. 여기서 pitch 를 걸면 2D 로 보던 사람이 클러스터를
            // 누를 때마다 지도가 기울어진다.
            mapViewportState.easeTo(
                cameraOptions {
                    center(cluster.anchorPoint())
                    zoom((mapViewportState.cameraState?.zoom ?: MapDetailDefaultZoom) + 1.5)
                },
                MapAnimationOptions.mapAnimationOptions { duration(600L) },
            )
        }
    }
    var is3d by rememberSaveable { mutableStateOf(false) }
    val on3dToggleClick: () -> Unit = remember(mapViewportState) {
        {
            val next = !is3d
            is3d = next
            mapViewportState.easeTo(
                cameraOptions { pitch(if (next) MapDetailPitch else 0.0) },
                MapAnimationOptions.mapAnimationOptions { duration(400L) },
            )
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        MapDetailContent(
            mapTitle = screenState.title ?: initialTitle,
            roleBadge = screenState.roleBadge,
            action = screenState.action,
            actionEnabled = !screenState.actionInProgress,
            placeCount = screenState.placeCount,
            is3d = is3d,
            canAddPlace = screenState.canAddPlace,
            selectedTab = uiState.selectedTab,
            onBackClick = onBackClick,
            onActionClick = {
                if (screenState.action == MapDetailAction.Join) viewModel.join() else viewModel.leave()
            },
            on3dToggleClick = on3dToggleClick,
            onAddPlaceClick = {
                // 시트를 닫아도 ViewModel 은 이 화면에 매여 살아남는다. 지우지 않으면
                // 다시 열었을 때 직전에 등록한 장소의 폼이 그대로 보인다.
                addPlaceViewModel.reset()
                addPlaceSheetVisible = true
            },
            onTabSelected = { tab -> uiState = uiState.selectTab(tab) },
            places = visiblePlaces,
            searchQuery = uiState.searchQuery,
            onSearchQueryChange = { query -> uiState = uiState.search(query) },
            onPlaceClick = { placeId -> uiState = uiState.selectPlace(placeId) },
            canReviewRequests = screenState.canReviewRequests,
            // TODO: 장소 등록 요청은 아직 목데이터다. `GET api/v1/places/pending` 이 붙으면 여기만 바꾼다.
            pendingRequests = SamplePendingRequests,
            logs = logs,
            logsLoading = activityState.loading,
            logsErrorMessage = activityState.errorMessage,
            onRequestAccept = {},
            onRequestReject = {},
            onLogsRetry = activityViewModel::retry,
            onMembersClick = { memberSheetVisible = true },
            mapContent = {
                MapDetailMap(
                    mapViewportState = mapViewportState,
                    markers = markers,
                    is3d = is3d,
                    onMarkerClick = onMarkerClick,
                    onClusterClick = onClusterClick,
                    modifier = Modifier.fillMaxSize(),
                )
            },
        )

        // 스낵바 자리는 하나뿐이라 두 출처를 한 줄로 모은다. 서버 실패가 먼저다.
        ErrorSnackbar(
            message = screenState.errorMessage ?: addPlaceNotice,
            onShown = {
                if (screenState.errorMessage != null) {
                    viewModel.consumeErrorMessage()
                } else {
                    addPlaceNotice = null
                }
            },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }

    selectedPlace?.let { place ->
        PlaceDetailSheet(
            place = place,
            reviews = reviews,
            onDismiss = closePlaceDetail,
            onRetryReviews = reviewViewModel::retry,
            // 참여 중인 지도에만 후기를 남길 수 있다. 서버도 같은 기준으로 막는다.
            onSubmitReview = if (screenState.canAddPlace) {
                { rating, reviewText -> reviewViewModel.submit(rating, reviewText) }
            } else {
                null
            },
        )
    }

    // 지도를 아직 못 읽었으면 열지 않는다. 버튼 글씨와 mapId 가 지도 정보에 달려 있다.
    val map = screenState.map.mapOrNull
    if (addPlaceSheetVisible && map != null) {
        AddPlaceSheet(
            map = map,
            viewModel = addPlaceViewModel,
            onDismiss = { addPlaceSheetVisible = false },
            onAdded = { message ->
                addPlaceSheetVisible = false
                addPlaceNotice = message
                // 장소 수가 늘었다. 상단과 시트 제목이 옛 값을 들고 있으면 안 된다.
                viewModel.retry()
            },
        )
    }

    if (memberSheetVisible) {
        MemberSheet(
            // TODO: 멤버 목록도 아직 목데이터다. 서버 API 가 생기면 여기만 바꾼다.
            members = SampleMembers,
            showRoles = screenState.showMemberRoles,
            canGrantRole = screenState.canGrantRole,
            onGrantRoleClick = {},
            onDismiss = { memberSheetVisible = false },
        )
    }
}

@Composable
internal fun MapDetailContent(
    mapTitle: String,
    roleBadge: String?,
    action: MapDetailAction,
    actionEnabled: Boolean,
    placeCount: Int?,
    is3d: Boolean,
    canAddPlace: Boolean,
    selectedTab: MapDetailTab,
    onBackClick: () -> Unit,
    onActionClick: () -> Unit,
    on3dToggleClick: () -> Unit,
    onAddPlaceClick: () -> Unit,
    onTabSelected: (MapDetailTab) -> Unit,
    places: List<PlaceUiModel>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onPlaceClick: (Long) -> Unit,
    canReviewRequests: Boolean,
    pendingRequests: List<PendingRequestUiModel>,
    logs: List<MapLogUiModel>,
    logsLoading: Boolean,
    logsErrorMessage: String?,
    onRequestAccept: (Long) -> Unit,
    onRequestReject: (Long) -> Unit,
    onLogsRetry: () -> Unit,
    onMembersClick: () -> Unit,
    modifier: Modifier = Modifier,
    mapContent: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MoaMapTheme.colors.backgroundSecondary)
            .statusBarsPadding(),
    ) {
        MapDetailTopBar(
            mapTitle = mapTitle,
            roleBadge = roleBadge,
            action = action,
            actionEnabled = actionEnabled,
            onBackClick = onBackClick,
            onActionClick = onActionClick,
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
        ) {
            when (selectedTab) {
                MapDetailTab.Places -> {
                    MapDetailPlacesContent(
                        places = places,
                        placeCount = placeCount,
                        searchQuery = searchQuery,
                        is3d = is3d,
                        canAddPlace = canAddPlace,
                        onSearchQueryChange = onSearchQueryChange,
                        onPlaceClick = onPlaceClick,
                        onTabSelected = onTabSelected,
                        on3dToggleClick = on3dToggleClick,
                        onAddPlaceClick = onAddPlaceClick,
                        mapContent = mapContent,
                    )
                }
                MapDetailTab.Logs -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        MapLogsContent(
                            // 알림을 띄울지는 여기서 정한다. MapLogsContent 는 받은 것만 그린다.
                            pendingRequests = if (canReviewRequests) pendingRequests else emptyList(),
                            // 지도 타입별로 거르지 않는다. 서버가 이미 프라이빗 지도에만
                            // 후기 로그를 넣어 보낸다.
                            logs = logs,
                            loading = logsLoading,
                            errorMessage = logsErrorMessage,
                            onAcceptClick = onRequestAccept,
                            onRejectClick = onRequestReject,
                            onRetryClick = onLogsRetry,
                        )
                        MapDetailTabBar(
                            selectedTab = selectedTab,
                            onTabSelected = onTabSelected,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(start = 20.dp, top = 16.dp, end = 20.dp),
                        )
                        MemberSheetFab(
                            onClick = onMembersClick,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = 20.dp, bottom = 20.dp),
                        )
                    }
                }
            }
        }
    }
}

/** 멤버 관리 진입. 로그 탭에만 있다 - 장소 탭은 같은 자리를 3D·장소 추가가 쓴다. */
@Composable
private fun MemberSheetFab(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(MoaMapPrimitiveColors.Blue500)
            .clickable(onClick = onClick)
            .padding(8.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_person),
            contentDescription = "멤버 관리",
            tint = MoaMapTheme.colors.textWhite,
            modifier = Modifier.size(32.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MapDetailPlacesContent(
    places: List<PlaceUiModel>,
    placeCount: Int?,
    searchQuery: String,
    is3d: Boolean,
    canAddPlace: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onPlaceClick: (Long) -> Unit,
    onTabSelected: (MapDetailTab) -> Unit,
    on3dToggleClick: () -> Unit,
    onAddPlaceClick: () -> Unit,
    mapContent: @Composable () -> Unit,
) {
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.PartiallyExpanded,
            skipHiddenState = true,
        ),
    )
    val scope = rememberCoroutineScope()

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetContent = {
            MapDetailBottomSheet(
                places = places,
                placeCount = placeCount,
                searchQuery = searchQuery,
                onSearchQueryChange = onSearchQueryChange,
                // 접힌 시트에서 검색창을 누르면 목록이 안 보인다. 눌린 김에 끝까지 올린다.
                onSearchFocused = { scope.launch { scaffoldState.bottomSheetState.expand() } },
                onPlaceClick = onPlaceClick,
            )
        },
        sheetPeekHeight = SheetPeekHeight,
        sheetShape = RoundedCornerShape(topStart = 38.dp, topEnd = 38.dp),
        sheetContainerColor = MoaMapTheme.colors.backgroundSecondary,
        sheetTonalElevation = 0.dp,
        sheetShadowElevation = 10.dp,
        sheetDragHandle = null,
        modifier = Modifier.fillMaxSize(),
    ) { _ ->
        Box(modifier = Modifier.fillMaxSize()) {
            mapContent()
            MapDetailTabBar(
                selectedTab = MapDetailTab.Places,
                onTabSelected = onTabSelected,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(start = 20.dp, top = 16.dp, end = 20.dp),
            )
            MapDetailMapControls(
                is3d = is3d,
                canAddPlace = canAddPlace,
                on3dToggleClick = on3dToggleClick,
                onAddPlaceClick = onAddPlaceClick,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = SheetPeekHeight + MapControlsBottomGap),
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun MapDetailScreenPreview() {
    MoaMapTheme {
        MapDetailContent(
            mapTitle = "서울 데이트 지도",
            roleBadge = "방장",
            action = MapDetailAction.Leave,
            actionEnabled = true,
            placeCount = 32,
            is3d = true,
            canAddPlace = true,
            selectedTab = MapDetailTab.Places,
            onBackClick = {},
            onActionClick = {},
            on3dToggleClick = {},
            onAddPlaceClick = {},
            onTabSelected = {},
            places = SamplePlaces,
            searchQuery = "",
            onSearchQueryChange = {},
            onPlaceClick = {},
            canReviewRequests = true,
            pendingRequests = SamplePendingRequests,
            logs = SampleMapLogs,
            logsLoading = false,
            logsErrorMessage = null,
            onRequestAccept = {},
            onRequestReject = {},
            onLogsRetry = {},
            onMembersClick = {},
            mapContent = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MoaMapPrimitiveColors.Blue50),
                )
            },
        )
    }
}

/** 미리보기로 들어온 상태. 참여하기가 뜨고 장소 추가는 잠겨 있다. */
@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun MapDetailScreenNotJoinedPreview() {
    MoaMapTheme {
        MapDetailContent(
            mapTitle = "성수 카페 투어",
            roleBadge = null,
            action = MapDetailAction.Join,
            actionEnabled = true,
            placeCount = 12,
            is3d = false,
            canAddPlace = false,
            selectedTab = MapDetailTab.Places,
            onBackClick = {},
            onActionClick = {},
            on3dToggleClick = {},
            onAddPlaceClick = {},
            onTabSelected = {},
            places = SamplePlaces,
            searchQuery = "",
            onSearchQueryChange = {},
            onPlaceClick = {},
            canReviewRequests = true,
            pendingRequests = SamplePendingRequests,
            logs = SampleMapLogs,
            logsLoading = false,
            logsErrorMessage = null,
            onRequestAccept = {},
            onRequestReject = {},
            onLogsRetry = {},
            onMembersClick = {},
            mapContent = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MoaMapPrimitiveColors.Blue50),
                )
            },
        )
    }
}
