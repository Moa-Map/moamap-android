package com.example.moamap.core.navigation

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
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.moamap.core.designsystem.component.ErrorSnackbar
import com.example.moamap.feature.collection.CollectionScreen
import com.example.moamap.feature.collection.domain.model.PlaceImportSource
import com.example.moamap.feature.collection.share.SharedLink
import com.example.moamap.feature.collection.presentation.createmap.CreateMapScreen
import com.example.moamap.feature.explore.ExploreScreen
import com.example.moamap.feature.footprint.watchrecord.WatchRecordScreen
import com.example.moamap.feature.mapdetail.MapDetailScreen
import com.example.moamap.feature.mapdetail.presentation.intro.MapIntroScreen
import com.example.moamap.feature.mypage.ProfileEditScreen
import com.example.moamap.feature.mypage.SettingsScreen
import com.example.moamap.feature.officialmap.OfficialMapScreen
import com.example.moamap.feature.officialmap.presentation.DensityMapDetailScreen
import com.example.moamap.feature.onboarding.presentation.LoginScreen
import com.example.moamap.feature.onboarding.presentation.SplashScreen

/**
 * @param pendingShare 다른 앱이 공유해 온 링크. 소비할 수 있을 때까지 상위가 들고 있는다.
 * @param onShareHandled [pendingShare] 를 처리했음을 알린다. 같은 링크로 두 번 들어가지 않게 한다.
 */
@Composable
internal fun MoaMapNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    pendingShare: SharedLink? = null,
    onShareHandled: () -> Unit = {},
) {
    val currentRoute by navController.currentBackStackEntryAsState()
    val destination = currentRoute?.destination?.route

    /** 공유가 지원하지 않는 링크였을 때의 안내. 어느 화면에도 매이지 않아 여기서 든다. */
    var shareError by remember { mutableStateOf<String?>(null) }

    /**
     * 공유로 들어온 링크를 장소 가져오기 흐름으로 넘긴다.
     *
     * 로그인 전에는 소비하지 않고 그대로 둔다. 로그인이 끝나 [MoaMapRoute.Explore] 로
     * 넘어오면 목적지가 바뀌면서 이 효과가 다시 돌아 그때 진입한다. 덕분에 "로그인
     * 끝나면 이어서" 를 위한 대기 상태를 따로 두지 않아도 된다.
     */
    LaunchedEffect(pendingShare, destination) {
        val share = pendingShare ?: return@LaunchedEffect
        if (
            destination == null ||
            destination == MoaMapRoute.Splash.route ||
            destination == MoaMapRoute.Login.route
        ) {
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
        ) {
            composable(MoaMapRoute.Splash.route) {
                SplashScreen(
                    onNavigateToLogin = { navController.replaceSplashWith(MoaMapRoute.Login) },
                    onNavigateToMain = { navController.replaceSplashWith(MoaMapRoute.Explore) },
                )
            }
            composable(MoaMapRoute.Login.route) {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate(MoaMapRoute.Explore.route) {
                            // 로그인 화면으로 되돌아갈 수 없게 지운다.
                            popUpTo(MoaMapRoute.Login.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(MoaMapRoute.Explore.route) {
                ExploreScreen(
                    onProfileEditClick = {
                        navController.navigate(MoaMapRoute.ProfileEdit.route)
                    },
                    onSettingsClick = {
                        navController.navigate(MoaMapRoute.Settings.route)
                    },
                    onWatchRecordClick = {
                        navController.navigate(MoaMapRoute.WatchRecord.route)
                    },
                    onOfficialMapClick = {
                        navController.navigate(MoaMapRoute.OfficialMap.route)
                    },
                    // 참여 중인 지도는 소개를 다시 볼 이유가 없다. 바로 상세로 보낸다.
                    onCommunityMapClick = { map ->
                        val route = if (map.joined) {
                            MoaMapRoute.MapDetail.createRoute(
                                mapId = map.id,
                                mapTitle = map.title,
                            )
                        } else {
                            MoaMapRoute.MapIntro.createRoute(mapId = map.id)
                        }
                        navController.navigate(route)
                    },
                )
            }
            composable(MoaMapRoute.Collection.route) {
                CollectionScreen(
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
                    onMapClick = { map ->
                        navController.navigate(
                            MoaMapRoute.MapDetail.createRoute(
                                mapId = map.id,
                                mapTitle = map.title,
                            )
                        )
                    },
                )
            }
            composable(MoaMapRoute.CreateMap.route) {
                CreateMapScreen(
                    onBackClick = navController::popBackStack,
                    // 만들기 화면을 백스택에 남기면 뒤로가기로 돌아와 같은 지도를 또 만들 수 있다.
                    // 만든 지도로 이동하는 건 지도 상세가 실제 데이터를 받은 뒤에 정한다.
                    onCreated = { navController.popBackStack() },
                )
            }
            placeImportGraph(navController)
            composable(MoaMapRoute.OfficialMap.route) {
                OfficialMapScreen(
                    onBackClick = navController::popBackStack,
                    onDensityMapClick = {
                        navController.navigate(MoaMapRoute.DensityMapDetail.route)
                    },
                )
            }
            composable(MoaMapRoute.DensityMapDetail.route) {
                DensityMapDetailScreen(onBackClick = navController::popBackStack)
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
                    onPreviewClick = {
                        navController.navigate(MoaMapRoute.MapDetail.createRoute(mapId))
                    },
                    // 참여하고 나면 소개 화면은 볼 일이 없다. 뒤로가기가 탐색 탭으로 가게 지운다.
                    onJoined = {
                        navController.navigate(MoaMapRoute.MapDetail.createRoute(mapId)) {
                            popUpTo(MoaMapRoute.MapIntro.route) { inclusive = true }
                        }
                    },
                )
            }
            composable(
                route = MoaMapRoute.MapDetail.route,
                arguments = listOf(
                    navArgument(MoaMapRoute.MapDetail.ARG_MAP_ID) { type = NavType.LongType },
                    navArgument(MoaMapRoute.MapDetail.ARG_MAP_TITLE) {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                ),
            ) { backStackEntry ->
                MapDetailScreen(
                    onBackClick = navController::popBackStack,
                    initialTitle = backStackEntry.arguments
                        ?.getString(MoaMapRoute.MapDetail.ARG_MAP_TITLE)
                        .orEmpty(),
                )
            }
            composable(MoaMapRoute.ProfileEdit.route) {
                ProfileEditScreen(onBackClick = navController::popBackStack)
            }
            composable(MoaMapRoute.Settings.route) {
                SettingsScreen(
                    onBackClick = navController::popBackStack,
                    onLoggedOut = { navController.navigateToLoginClearingStack() },
                )
            }
            composable(MoaMapRoute.WatchRecord.route) {
                WatchRecordScreen(onBackClick = navController::popBackStack)
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

            if (
                destination == MoaMapRoute.Explore.route ||
                destination == MoaMapRoute.Collection.route
            ) {
                MoaMapBottomBar(
                    currentRoute = destination,
                    onItemClick = { route -> navController.navigateToTab(route) },
                )
            }
        }
    }
}

private const val UNSUPPORTED_SHARE_MESSAGE = "인스타그램과 네이버·카카오·구글 지도 링크만 가져올 수 있어요"

/** 스플래시는 뒤로가기로 돌아올 곳이 아니므로 백스택에서 지우고 이동한다. */
private fun NavHostController.replaceSplashWith(destination: MoaMapRoute) {
    navigate(destination.route) {
        popUpTo(MoaMapRoute.Splash.route) { inclusive = true }
        launchSingleTop = true
    }
}

/**
 * 로그아웃 후 로그인 화면으로 보낸다.
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
