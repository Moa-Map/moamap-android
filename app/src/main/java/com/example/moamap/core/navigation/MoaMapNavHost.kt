package com.example.moamap.core.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.moamap.feature.collection.CollectionScreen
import com.example.moamap.feature.collection.presentation.createmap.CreateMapScreen
import com.example.moamap.feature.explore.ExploreScreen
import com.example.moamap.feature.mapdetail.MapDetailScreen
import com.example.moamap.feature.mypage.ProfileEditScreen
import com.example.moamap.feature.mypage.SettingsScreen
import com.example.moamap.feature.officialmap.OfficialMapScreen
import com.example.moamap.feature.officialmap.presentation.DensityMapDetailScreen
import com.example.moamap.feature.onboarding.presentation.LoginScreen
import com.example.moamap.feature.onboarding.presentation.SplashScreen

@Composable
fun MoaMapNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val currentRoute by navController.currentBackStackEntryAsState()

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
                    onOfficialMapClick = {
                        navController.navigate(MoaMapRoute.OfficialMap.route)
                    },
                    onCommunityMapClick = { map ->
                        navController.navigate(
                            MoaMapRoute.MapDetail.createRoute(
                                mapId = map.id,
                                mapTitle = map.title,
                            )
                        )
                    },
                )
            }
            composable(MoaMapRoute.Collection.route) {
                CollectionScreen(
                    onNewMapClick = {
                        navController.navigate(MoaMapRoute.CreateMap.route)
                    },
                    onInstagramImportClick = {
                        navController.navigate(MoaMapRoute.PlaceImport.route)
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
                route = MoaMapRoute.MapDetail.route,
                arguments = listOf(
                    navArgument(MoaMapRoute.MapDetail.ARG_MAP_ID) { type = NavType.LongType },
                    navArgument(MoaMapRoute.MapDetail.ARG_MAP_TITLE) { type = NavType.StringType },
                ),
            ) { backStackEntry ->
                MapDetailScreen(
                    mapTitle = backStackEntry.arguments
                        ?.getString(MoaMapRoute.MapDetail.ARG_MAP_TITLE)
                        .orEmpty(),
                    onBackClick = navController::popBackStack,
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
        }

        if (
            currentRoute?.destination?.route == MoaMapRoute.Explore.route ||
            currentRoute?.destination?.route == MoaMapRoute.Collection.route
        ) {
            MoaMapBottomBar(
                currentRoute = currentRoute?.destination?.route,
                onItemClick = { route -> navController.navigateToTab(route) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 12.dp),
            )
        }
    }
}

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
