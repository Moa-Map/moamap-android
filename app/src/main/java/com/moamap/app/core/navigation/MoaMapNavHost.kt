package com.moamap.app.core.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.moamap.app.core.common.MAP_CREATED_TOAST
import com.moamap.app.core.designsystem.component.ErrorSnackbar
import com.moamap.app.core.designsystem.component.MoaMapToast
import com.moamap.app.feature.collection.CollectionScreen
import com.moamap.app.feature.collection.domain.model.PlaceImportSource
import com.moamap.app.feature.collection.presentation.createmap.CreateMapScreen
import com.moamap.app.feature.collection.presentation.createmap.decodeImportedPlaces
import com.moamap.app.feature.collection.presentation.createmap.encodeImportedPlaces
import com.moamap.app.feature.collection.share.SharedLink
import com.moamap.app.feature.explore.CommunityMapListScreen
import com.moamap.app.feature.explore.ExploreScreen
import com.moamap.app.feature.mapdetail.MapDetailScreen
import com.moamap.app.feature.mapdetail.presentation.intro.MapIntroScreen
import com.moamap.app.feature.mypage.InquiryScreen
import com.moamap.app.feature.mypage.NoticeDetailScreen
import com.moamap.app.feature.mypage.NoticeListScreen
import com.moamap.app.feature.mypage.ProfileEditScreen
import com.moamap.app.feature.mypage.SettingsScreen
import com.moamap.app.feature.notification.NotificationScreen
import com.moamap.app.feature.notification.presentation.NotificationDestination
import com.moamap.app.feature.officialmap.OfficialMapScreen
import com.moamap.app.feature.officialmap.domain.model.isDensityOfficialMap
import com.moamap.app.feature.officialmap.domain.model.isRestroomOfficialMap
import com.moamap.app.feature.officialmap.presentation.DensityMapDetailScreen
import com.moamap.app.feature.officialmap.presentation.RestroomMapScreen
import com.moamap.app.feature.onboarding.presentation.LoginScreen
import com.moamap.app.feature.onboarding.presentation.SplashScreen
import com.moamap.app.feature.terms.TermsAgreementScreen
import com.moamap.app.feature.terms.TermsDetailScreen
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * @param pendingShare 다른 앱이 공유해 온 링크. 소비할 수 있을 때까지 상위가 들고 있는다.
 * @param onShareHandled [pendingShare] 를 처리했음을 알린다. 같은 링크로 두 번 들어가지 않게 한다.
 * @param sessionExpired 서버가 세션을 끝냈다는 신호. 받으면 로그인 화면으로 보낸다.
 */
@Composable
internal fun MoaMapNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    pendingShare: SharedLink? = null,
    onShareHandled: () -> Unit = {},
    sessionExpired: Flow<Unit> = emptyFlow(),
) {
    val currentRoute by navController.currentBackStackEntryAsState()
    val destination = currentRoute?.destination?.route

    /**
     * 세션이 끝나면 로그인 화면으로 보낸다.
     *
     * 로그인 전 화면에서는 무시한다. 스플래시는 토큰이 없으면 스스로 로그인으로 가고, 로그인 화면은 이미 도착한
     * 곳이며, 약관 동의 화면은 401 을 스스로 받아 로그인으로 간다. 지금 목적지는 수집 시점에 읽는다 - [destination] 을
     * 키로 두면 화면이 바뀔 때마다 수집이 끊겼다 다시 붙는다.
     */
    LaunchedEffect(sessionExpired) {
        sessionExpired.collect {
            if (navController.isOnPreLoginScreen()) {
                return@collect
            }
            navController.navigateToLoginClearingStack()
        }
    }

    /** 공유가 지원하지 않는 링크였을 때의 안내. 어느 화면에도 매이지 않아 여기서 든다. */
    var shareError by remember { mutableStateOf<String?>(null) }

    // 모음 편집에서 지도를 고르면 그 화면이 하단 탭 자리에 선택 막대를 띄운다.
    var collectionSelectionBarVisible by remember { mutableStateOf(false) }
    /**
     * 탐색·모음 탭 위에 띄우는 완료 토스트. 시안대로 하단 탭 위 끝에 18 겹쳐 그려야 해서, 하단 탭을 그리는
     * 이 자리에서 탭보다 나중에 그린다. 화면 안에 두면 탭에 가린다.
     */
    var tabToast by remember { mutableStateOf<String?>(null) }

    /**
     * 공유로 들어온 링크를 장소 가져오기 흐름으로 넘긴다.
     *
     * 로그인 전에는 소비하지 않고 그대로 둔다. 로그인이 끝나 [MoaMapRoute.Explore] 로
     * 넘어오면 목적지가 바뀌면서 이 효과가 다시 돌아 그때 진입한다. 덕분에 "로그인
     * 끝나면 이어서" 를 위한 대기 상태를 따로 두지 않아도 된다.
     */
    LaunchedEffect(pendingShare, destination) {
        val share = pendingShare ?: return@LaunchedEffect
        if (destination == null || navController.isOnPreLoginScreen()) {
            return@LaunchedEffect
        }

        when (share) {
            is SharedLink.Supported -> navController.navigate(
                MoaMapRoute.PlaceImport.createRoute(share.source.name, share.url),
            )

            // 흐름을 열어봐야 할 수 있는 것이 없다. 모음 탭에서 이유만 알린다.
            SharedLink.Unsupported -> {
                navController.navigateToTab(MoaMapRoute.Collection)
                shareError = UNSUPPORTED_SHARE_MESSAGE
            }
        }
        onShareHandled()
    }

    Box(modifier = modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = MoaMapRoute.Splash.route,
            // 기본 전환은 두 화면을 겹쳐 놓고 투명도를 바꾼다. 그동안 이전 화면이 비쳐
            // 잔상처럼 보여서 전부 끈다. 여기 넣은 값이 중첩 그래프까지 기본값이 된다.
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None },
        ) {
            composable(MoaMapRoute.Splash.route) {
                SplashScreen(
                    onNavigateToLogin = { navController.replaceSplashWith(MoaMapRoute.Login) },
                    onNavigateToTermsAgreement = { navController.replaceSplashWith(MoaMapRoute.TermsAgreement) },
                    onNavigateToMain = { navController.replaceSplashWith(MoaMapRoute.Explore) },
                )
            }
            composable(MoaMapRoute.Login.route) {
                LoginScreen(
                    // 서버가 동의할 약관이 남았다고 하면 동의 화면으로, 아니면 홈으로 간다.
                    onLoginSuccess = { needsTermsAgreement ->
                        if (needsTermsAgreement) {
                            navController.navigate(MoaMapRoute.TermsAgreement.route) {
                                launchSingleTop = true
                            }
                        } else {
                            navController.navigateClearingPreLogin(MoaMapRoute.Explore)
                        }
                    },
                )
            }
            composable(MoaMapRoute.TermsAgreement.route) {
                TermsAgreementScreen(
                    onAgreed = { navController.navigateClearingPreLogin(MoaMapRoute.Explore) },
                    // 스플래시에서 바로 왔으면 아래에 로그인 화면이 없다. 어느 쪽이든 로그인 화면 하나만 남긴다.
                    onCancelled = { navController.navigateClearingPreLogin(MoaMapRoute.Login) },
                    onTermsClick = { termId ->
                        navController.navigate(MoaMapRoute.TermsDetail.createRoute(termId))
                    },
                )
            }
            composable(
                route = MoaMapRoute.TermsDetail.route,
                arguments = listOf(
                    navArgument(MoaMapRoute.TermsDetail.ARG_TERM_ID) { type = NavType.LongType },
                ),
            ) {
                TermsDetailScreen(onBackClick = navController::popBackStack)
            }
            composable(MoaMapRoute.Explore.route) { entry ->
                // 모음에서 로고로 들어오면 보던 자리가 아니라 맨 위에서 시작한다.
                val scrollToTop by entry.savedStateHandle
                    .getStateFlow(ScrollToTopKey, false)
                    .collectAsStateWithLifecycle()

                ExploreScreen(
                    scrollToTop = scrollToTop,
                    onScrolledToTop = { entry.savedStateHandle[ScrollToTopKey] = false },
                    onProfileEditClick = {
                        navController.navigate(MoaMapRoute.ProfileEdit.route)
                    },
                    onSettingsClick = {
                        navController.navigate(MoaMapRoute.Settings.route)
                    },
                    onNotificationClick = {
                        navController.navigate(MoaMapRoute.Notifications.route)
                    },
                    onCommunityMapClick = { map ->
                        navController.navigateToMap(mapId = map.id, mapTitle = map.title, joined = map.joined)
                    },
                    onSeeAllCommunityMapsClick = {
                        navController.navigate(MoaMapRoute.CommunityMaps.route)
                    },
                    // 공식지도 목록에서 누를 때와 같은 흐름이다.
                    onOfficialMapClick = { map ->
                        navController.navigateToMap(
                            mapId = map.id,
                            mapTitle = map.title,
                            joined = map.joined,
                            official = true,
                        )
                    },
                    onSeeAllOfficialMapsClick = {
                        navController.navigate(MoaMapRoute.OfficialMap.route)
                    },
                )
            }
            composable(MoaMapRoute.CommunityMaps.route) {
                CommunityMapListScreen(
                    onBackClick = navController::popBackStack,
                    // 검색 결과에는 공식지도가 섞여 온다. 홈의 공식 카드와 같은 길로 보내야 유동인구·화장실 화면으로 간다.
                    onMapClick = { map ->
                        navController.navigateToMap(
                            mapId = map.id,
                            mapTitle = map.title,
                            joined = map.joined,
                            official = map.official,
                        )
                    },
                )
            }
            composable(MoaMapRoute.Collection.route) { entry ->
                val createMapNotice by entry.savedStateHandle
                    .getStateFlow<String?>(CreateMapNoticeKey, null)
                    .collectAsStateWithLifecycle()

                CollectionScreen(
                    pendingNotice = createMapNotice,
                    onPendingNoticeConsumed = { entry.savedStateHandle[CreateMapNoticeKey] = null },
                    onToast = { message -> tabToast = message },
                    onSelectionBarVisibleChange = { visible -> collectionSelectionBarVisible = visible },
                    // 로고는 홈(탐색)으로 가는 버튼이다. 하단 탭으로 옮기는 것과 같게 옮기되,
                    // 홈은 보던 자리 대신 맨 위에서 시작한다.
                    onHomeClick = {
                        navController.navigateToTab(MoaMapRoute.Explore)
                        navController.getBackStackEntry(MoaMapRoute.Explore.route)
                            .savedStateHandle[ScrollToTopKey] = true
                    },
                    onNewMapClick = {
                        navController.navigate(MoaMapRoute.CreateMap.route)
                    },
                    onInstagramImportClick = {
                        navController.navigate(
                            MoaMapRoute.PlaceImport.createRoute(PlaceImportSource.Instagram.name),
                        )
                    },
                    onMapShareImportClick = {
                        navController.navigate(
                            MoaMapRoute.PlaceImport.createRoute(PlaceImportSource.MapShare.name),
                        )
                    },
                    // 모음에 있는 지도는 모두 참여한 지도다. 참여한 공식지도도 여기 섞여 있다.
                    onMapClick = { map ->
                        navController.navigateToMap(
                            mapId = map.id,
                            mapTitle = map.title,
                            joined = true,
                            official = map.official,
                        )
                    },
                )
            }
            composable(MoaMapRoute.CreateMap.route) { entry ->
                val importedPlaces by entry.savedStateHandle
                    .getStateFlow<String?>(ImportedPlacesForNewMapKey, null)
                    .collectAsStateWithLifecycle()
                val decodedPlaces = remember(importedPlaces) {
                    importedPlaces?.let(::decodeImportedPlaces)
                }

                CreateMapScreen(
                    onBackClick = navController::popBackStack,
                    // 만들기 화면을 백스택에 남기면 뒤로가기로 돌아와 같은 지도를 또 만들 수 있다.
                    // 만든 지도로 이동하는 건 지도 상세가 실제 데이터를 받은 뒤에 정한다.
                    // 장소를 다 넣지 못했으면 돌아간 모음 탭이 안내하도록 넘긴다.
                    onCreated = { _, importNotice ->
                        tabToast = MAP_CREATED_TOAST
                        if (importNotice != null) {
                            navController.previousBackStackEntry
                                ?.savedStateHandle
                                ?.set(CreateMapNoticeKey, importNotice)
                        }
                        navController.popBackStack()
                    },
                    onExternalImportClick = {
                        navController.navigate(
                            MoaMapRoute.PlaceImport.createRoute(
                                source = PlaceImportSource.MapShare.name,
                                forNewMap = true,
                            ),
                        )
                    },
                    importedPlaces = decodedPlaces,
                    onImportedPlacesConsumed = {
                        entry.savedStateHandle[ImportedPlacesForNewMapKey] = null
                    },
                )
            }
            // 새 지도용으로 열린 흐름은 고른 장소를 만들기 화면에 넘기고 통째로 닫는다.
            // Bundle 에 담겨 프로세스가 죽어도 남도록 문자열로 넘긴다.
            placeImportGraph(
                navController = navController,
                onImportedForNewMap = { places ->
                    navController.getBackStackEntry(MoaMapRoute.CreateMap.route)
                        .savedStateHandle[ImportedPlacesForNewMapKey] = encodeImportedPlaces(places)
                    navController.popBackStack(MoaMapRoute.PlaceImport.route, inclusive = true)
                },
                onImportDone = { toast -> tabToast = toast },
            )
            composable(MoaMapRoute.OfficialMap.route) {
                OfficialMapScreen(
                    onBackClick = navController::popBackStack,
                    onMapClick = { map ->
                        navController.navigateToMap(
                            mapId = map.id,
                            mapTitle = map.title,
                            joined = map.joined,
                            official = true,
                        )
                    },
                )
            }
            composable(
                route = MoaMapRoute.DensityMapDetail.route,
                arguments = MapScreenArguments,
            ) { backStackEntry ->
                DensityMapDetailScreen(
                    onBackClick = navController::popBackStack,
                    initialTitle = backStackEntry.mapTitleArgument(),
                )
            }
            composable(
                route = MoaMapRoute.RestroomMapDetail.route,
                arguments = MapScreenArguments,
            ) { backStackEntry ->
                RestroomMapScreen(
                    onBackClick = navController::popBackStack,
                    initialTitle = backStackEntry.mapTitleArgument(),
                )
            }
            composable(
                route = MoaMapRoute.MapIntro.route,
                arguments = listOf(
                    navArgument(MoaMapRoute.MapIntro.ARG_MAP_ID) { type = NavType.LongType },
                ),
            ) { backStackEntry ->
                val mapId = backStackEntry.arguments
                    ?.getLong(MoaMapRoute.MapIntro.ARG_MAP_ID) ?: 0L

                MapIntroScreen(
                    onBackClick = navController::popBackStack,
                    // 미리보기는 소개 화면을 백스택에 남긴다. 뒤로가면 다시 소개로 돌아온다.
                    onPreviewClick = { mapTitle, official ->
                        navController.navigate(mapScreenRoute(mapId, mapTitle, official))
                    },
                    // 참여하고 나면 소개 화면은 볼 일이 없다. 뒤로가기가 탐색 탭으로 가게 지운다.
                    onJoined = { mapTitle, official ->
                        navController.navigate(mapScreenRoute(mapId, mapTitle, official, joined = true)) {
                            popUpTo(MoaMapRoute.MapIntro.route) { inclusive = true }
                        }
                    },
                )
            }
            composable(
                route = MoaMapRoute.MapDetail.route,
                arguments = MapScreenArguments + navArgument(MoaMapRoute.MapDetail.ARG_POST_ID) {
                    type = NavType.LongType
                    defaultValue = MoaMapRoute.MapDetail.NO_POST_ID
                },
            ) { backStackEntry ->
                MapDetailScreen(
                    // 미리보기를 거쳐 들어와 참여했으면 소개 화면은 볼 일이 없다. 함께 지워
                    // 들어왔던 화면(탐색 또는 모음)으로 돌려보낸다. 소개를 거치지 않았으면
                    // 지울 것이 없어 한 칸만 돌아간다.
                    onBackClick = { joinedHere ->
                        val skippedIntro = joinedHere &&
                            navController.popBackStack(MoaMapRoute.MapIntro.route, inclusive = true)
                        if (!skippedIntro) navController.popBackStack()
                    },
                    initialTitle = backStackEntry.mapTitleArgument(),
                )
            }
            composable(MoaMapRoute.ProfileEdit.route) {
                ProfileEditScreen(onBackClick = navController::popBackStack)
            }
            composable(MoaMapRoute.Settings.route) {
                SettingsScreen(
                    onBackClick = navController::popBackStack,
                    onLoggedOut = { navController.navigateToLoginClearingStack() },
                    onOpenTerms = { termId ->
                        navController.navigate(MoaMapRoute.TermsDetail.createRoute(termId))
                    },
                    onInquiryClick = { navController.navigate(MoaMapRoute.Inquiry.route) },
                    onNoticeClick = { navController.navigate(MoaMapRoute.Notices.route) },
                )
            }
            composable(MoaMapRoute.Inquiry.route) {
                InquiryScreen(onBackClick = navController::popBackStack)
            }
            composable(MoaMapRoute.Notifications.route) {
                NotificationScreen(
                    onBackClick = navController::popBackStack,
                    // 알림이 가리키는 지도는 참여한 커뮤니티·프라이빗 지도다. 특수 공식지도 화면으로 갈 일은 없다.
                    onDestination = { destination ->
                        val route = when (destination) {
                            is NotificationDestination.Map -> MoaMapRoute.MapDetail.createRoute(destination.mapId)
                            is NotificationDestination.Post ->
                                MoaMapRoute.MapDetail.createRoute(destination.mapId, postId = destination.postId)
                        }
                        navController.navigate(route)
                    },
                )
            }
            composable(MoaMapRoute.Notices.route) {
                NoticeListScreen(
                    onBackClick = navController::popBackStack,
                    onNoticeClick = { noticeId ->
                        navController.navigate(MoaMapRoute.NoticeDetail.createRoute(noticeId))
                    },
                )
            }
            composable(
                route = MoaMapRoute.NoticeDetail.route,
                arguments = listOf(
                    navArgument(MoaMapRoute.NoticeDetail.ARG_NOTICE_ID) { type = NavType.LongType },
                ),
            ) {
                NoticeDetailScreen(onBackClick = navController::popBackStack)
            }
        }

        // 안내가 하단바에 가리지 않도록 위에 쌓는다. 띄울 것이 없으면 높이를 차지하지 않는다.
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 12.dp),
        ) {
            ErrorSnackbar(
                message = shareError,
                onShown = { shareError = null },
            )

            val collectionSelecting =
                destination == MoaMapRoute.Collection.route && collectionSelectionBarVisible
            if (
                (destination == MoaMapRoute.Explore.route ||
                    destination == MoaMapRoute.Collection.route) &&
                !collectionSelecting
            ) {
                MoaMapBottomBar(
                    currentRoute = destination,
                    onItemClick = { route -> navController.navigateToTab(route) },
                )
            }
        }

        // 시안 「모음 프라이빗」: 토스트 아래 끝이 하단 탭 위 끝보다 18 아래다(하단 탭 아래 12 + 높이 58 - 18).
        MoaMapToast(
            message = tabToast,
            onShown = { tabToast = null },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = TabToastBottomPadding),
        )
    }
}

/**
 * 지도 화면 경로(지도 상세·유동인구·화장실)의 인자. 셋 다 [MoaMapRoute.MapDetail] 의 이름을 쓴다.
 */
private val MapScreenArguments = listOf(
    navArgument(MoaMapRoute.MapDetail.ARG_MAP_ID) { type = NavType.LongType },
    navArgument(MoaMapRoute.MapDetail.ARG_MAP_TITLE) {
        type = NavType.StringType
        defaultValue = ""
    },
    navArgument(MoaMapRoute.MapDetail.ARG_JOINED) {
        type = NavType.BoolType
        defaultValue = false
    },
)

/** 서버 응답이 오기 전까지 상단 바를 채울 지도 이름. 없으면 빈 문자열. */
private fun NavBackStackEntry.mapTitleArgument(): String =
    arguments?.getString(MoaMapRoute.MapDetail.ARG_MAP_TITLE).orEmpty()

/**
 * 아직 로그인을 마치지 않은 화면. 세션 만료로 로그인 화면에 보내지 않고, 공유 링크도 처리하지 않는다 -
 * 약관 동의를 마치기 전에 장소 가져오기로 넘어가면 안 된다.
 */
private val PreLoginRoutes = setOf(
    MoaMapRoute.Splash.route,
    MoaMapRoute.Login.route,
    MoaMapRoute.TermsAgreement.route,
)

/**
 * 지금 화면이 로그인을 마치기 전인지. 경로가 없으면(그래프를 붙이기 전) 로그인 전으로 본다.
 *
 * 약관 전문은 경로만으로 가를 수 없다. 동의 화면 위에서 열면 동의를 마치기 전이고, 설정에서 열면 로그인 후다. 그래서
 * 아래에 동의 화면이 깔려 있는지를 함께 본다.
 */
internal fun isPreLoginScreen(route: String?, termsAgreementInBackStack: Boolean): Boolean = when (route) {
    null -> true
    in PreLoginRoutes -> true
    MoaMapRoute.TermsDetail.route -> termsAgreementInBackStack
    else -> false
}

private fun NavHostController.isOnPreLoginScreen(): Boolean {
    val route = currentDestination?.route
    val termsAgreementInBackStack = route == MoaMapRoute.TermsDetail.route &&
        runCatching { getBackStackEntry(MoaMapRoute.TermsAgreement.route) }.isSuccess
    return isPreLoginScreen(route, termsAgreementInBackStack)
}

private const val UNSUPPORTED_SHARE_MESSAGE = "인스타그램과 네이버·카카오·구글 지도 링크만 가져올 수 있어요"

/**
 * 홈을 맨 위로 올리라는 신호. 탐색 화면이 읽고 나서 스스로 끈다.
 *
 * 탭 이동은 보던 자리를 복원하는데(`navigateToTab`), 로고로 들어올 때는 홈을 처음 모습으로
 * 보여줘야 해서 한 번짜리 신호를 남긴다.
 */
private const val ScrollToTopKey = "explore_scroll_to_top"

/** 새 지도 만들기가 모음 탭에 돌려주는 안내. 불러온 장소를 다 넣지 못했을 때만 온다. */
private const val CreateMapNoticeKey = "collection_create_map_notice"

private val TabToastBottomPadding = 52.dp

/** 장소 가져오기 흐름이 새 지도 만들기 화면에 돌려주는 결과. */
private const val ImportedPlacesForNewMapKey = "create_map_imported_places"

/** 스플래시는 뒤로가기로 돌아올 곳이 아니므로 백스택에서 지우고 이동한다. */
private fun NavHostController.replaceSplashWith(destination: MoaMapRoute) {
    navigate(destination.route) {
        popUpTo(MoaMapRoute.Splash.route) { inclusive = true }
        launchSingleTop = true
    }
}

/**
 * 로그인 전 화면(로그인·약관 동의)을 모두 지우고 [destination] 으로 간다. 홈에서 뒤로 가 동의 화면으로, 동의를 취소한 뒤
 * 뒤로 가 동의 화면으로 돌아가지 않게 한다.
 */
private fun NavHostController.navigateClearingPreLogin(destination: MoaMapRoute) {
    navigate(destination.route) {
        popUpTo(graph.id) { inclusive = true }
        launchSingleTop = true
    }
}

/**
 * 로그아웃하거나 세션이 끝난 뒤 로그인 화면으로 보낸다.
 *
 * 로그인 이후 화면의 뿌리는 [MoaMapRoute.Explore] 다. 여기까지 inclusive 로 비우면
 * 뒤로가기로 로그인 전 화면에 접근할 수 없다.
 */
private fun NavHostController.navigateToLoginClearingStack() {
    navigate(MoaMapRoute.Login.route) {
        popUpTo(MoaMapRoute.Explore.route) { inclusive = true }
        launchSingleTop = true
    }
}

/**
 * 탭 전환 시 백스택이 쌓이지 않도록 [MoaMapRoute.Explore] 까지 popUp 하고,
 * 이전에 보던 탭 상태는 복원한다.
 *
 * 그래프의 시작 지점이 아니라 Explore 를 기준으로 삼는다 - 시작 지점인 스플래시는
 * 이동 직후 백스택에서 제거되므로 popUpTo 대상이 될 수 없다.
 */
private fun NavHostController.navigateToTab(route: MoaMapRoute) {
    navigate(route.route) {
        popUpTo(MoaMapRoute.Explore.route) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/**
 * 지도 카드를 눌렀을 때. 참여 중인 지도는 소개를 다시 볼 이유가 없어 바로 상세로 보낸다.
 *
 * 탐색 탭·커뮤니티 지도 전체보기·공식지도 목록·모음 탭이 같은 규칙을 쓴다. 유동인구 지도는
 * 소개할 내용이 없어 참여 여부와 상관없이 바로 유동인구 화면으로 간다.
 */
private fun NavHostController.navigateToMap(
    mapId: Long,
    mapTitle: String,
    joined: Boolean,
    official: Boolean = false,
) {
    val route = if (joined || isDensityOfficialMap(official, mapTitle)) {
        mapScreenRoute(mapId = mapId, mapTitle = mapTitle, official = official)
    } else {
        MoaMapRoute.MapIntro.createRoute(mapId = mapId)
    }
    navigate(route)
}

/**
 * 지도 자체를 보는 화면. 장소가 없는 특수 공식지도(유동인구·공중화장실)는 지도 상세 대신
 * 전용 화면이다. 소개 화면의 미리보기·참여 후 이동도 이 규칙을 쓴다.
 */
private fun mapScreenRoute(
    mapId: Long,
    mapTitle: String,
    official: Boolean,
    joined: Boolean = false,
): String = when {
    isDensityOfficialMap(official, mapTitle) ->
        MoaMapRoute.DensityMapDetail.createRoute(mapId = mapId, mapTitle = mapTitle, joined = joined)
    isRestroomOfficialMap(official, mapTitle) ->
        MoaMapRoute.RestroomMapDetail.createRoute(mapId = mapId, mapTitle = mapTitle, joined = joined)
    else -> MoaMapRoute.MapDetail.createRoute(mapId = mapId, mapTitle = mapTitle, joined = joined)
}
