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
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.moamap.feature.collection.CollectionScreen
import com.example.moamap.feature.explore.ExploreScreen
import com.example.moamap.feature.mapdetail.MapDetailScreen
import com.example.moamap.feature.mypage.ProfileEditScreen
import com.example.moamap.feature.mypage.SettingsScreen
import com.example.moamap.feature.officialmap.OfficialMapScreen

@Composable
fun MoaMapNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val currentRoute by navController.currentBackStackEntryAsState()

    Box(modifier = modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = MoaMapRoute.Explore.route,
        ) {
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
                    onFirstCommunityMapClick = {
                        navController.navigate(MoaMapRoute.MapDetail.route)
                    },
                )
            }
            composable(MoaMapRoute.Collection.route) { CollectionScreen() }
            composable(MoaMapRoute.OfficialMap.route) {
                OfficialMapScreen(onBackClick = navController::popBackStack)
            }
            composable(MoaMapRoute.MapDetail.route) {
                MapDetailScreen(
                    mapTitle = "서울 팝업스토어 맵",
                    onBackClick = navController::popBackStack,
                )
            }
            composable(MoaMapRoute.ProfileEdit.route) {
                ProfileEditScreen(onBackClick = navController::popBackStack)
            }
            composable(MoaMapRoute.Settings.route) {
                SettingsScreen(onBackClick = navController::popBackStack)
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

/**
 * 탭 전환 시 백스택이 쌓이지 않도록 시작 지점까지 popUp 하고,
 * 이전에 보던 탭 상태는 복원한다.
 */
private fun NavHostController.navigateToTab(route: MoaMapRoute) {
    navigate(route.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
