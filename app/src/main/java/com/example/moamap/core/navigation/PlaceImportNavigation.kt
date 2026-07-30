package com.example.moamap.core.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import com.example.moamap.feature.collection.presentation.placeimport.ExtractionState
import com.example.moamap.feature.collection.presentation.placeimport.PlaceImportEditScreen
import com.example.moamap.feature.collection.presentation.placeimport.PlaceImportLoadingScreen
import com.example.moamap.feature.collection.presentation.placeimport.PlaceImportMapScreen
import com.example.moamap.feature.collection.presentation.placeimport.PlaceImportPlaceScreen
import com.example.moamap.feature.collection.presentation.placeimport.PlaceImportRoute
import com.example.moamap.feature.collection.presentation.placeimport.PlaceImportUrlScreen
import com.example.moamap.feature.collection.presentation.placeimport.PlaceImportViewModel

/**
 * 링크로 장소를 가져오는 5단계 흐름. URL 입력 → 로딩 → 장소 선택 → 편집 → 지도 선택.
 *
 * 다섯 화면이 그래프 back stack entry 에 스코프한 [PlaceImportViewModel] 하나를 공유한다.
 * 흐름을 벗어나면 ViewModel 이 함께 정리되므로 다음에 다시 들어와도 이전 입력이 남지 않는다.
 */
internal fun NavGraphBuilder.placeImportGraph(navController: NavHostController) {
    navigation(
        route = MoaMapRoute.PlaceImport.route,
        startDestination = PlaceImportRoute.URL,
        arguments = listOf(
            navArgument(MoaMapRoute.PlaceImport.ARG_SOURCE) { type = NavType.StringType },
            navArgument(MoaMapRoute.PlaceImport.ARG_URL) {
                type = NavType.StringType
                defaultValue = ""
            },
        ),
    ) {
        composable(PlaceImportRoute.URL) { entry ->
            val viewModel = sharedPlaceImportViewModel(navController, entry)
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            // 임시. 워치 기록에서 들어오면 입력받을 링크가 없어 로딩부터 시작한다.
            // 여기를 pop 하면 그래프의 시작 화면이 사라지므로 백스택에는 그대로 남겨두고,
            // 대신 뒤 화면들의 뒤로가기가 그래프를 통째로 빠져나간다.
            if (uiState.source.isWalkRecord) {
                LaunchedEffect(Unit) {
                    viewModel.startExtraction()
                    navController.navigate(PlaceImportRoute.LOADING) { launchSingleTop = true }
                }

                // 넘어가기까지 한 프레임이 비므로 로딩 화면을 미리 같은 모습으로 그려둔다.
                PlaceImportLoadingScreen(
                    source = uiState.source,
                    onCancel = {
                        viewModel.cancelExtraction()
                        navController.popBackStack(MoaMapRoute.PlaceImport.route, inclusive = true)
                    },
                )
                return@composable
            }

            PlaceImportUrlScreen(
                url = uiState.url,
                canSearch = uiState.canSearch,
                errorMessage = uiState.errorMessage,
                onUrlChange = viewModel::updateUrl,
                onBackClick = navController::popBackStack,
                onErrorShown = viewModel::consumeError,
                onSearchClick = {
                    viewModel.startExtraction()
                    navController.navigate(PlaceImportRoute.LOADING)
                },
            )
        }

        composable(PlaceImportRoute.LOADING) { entry ->
            val viewModel = sharedPlaceImportViewModel(navController, entry)
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            // 추출이 끝나면 로딩 화면을 백스택에서 지우고 넘어간다.
            // 남겨두면 장소 선택에서 뒤로갈 때 이미 끝난 로딩 화면이 다시 보인다.
            //
            // 재시도로 들어온 경우에는 아래에 이전 장소 화면이 남아 있다. launchSingleTop 으로
            // 그 화면을 재사용해 재시도를 반복해도 백스택이 자라지 않게 한다.
            LaunchedEffect(uiState.extraction, uiState.errorMessage) {
                when {
                    // 실패하면 직전 화면으로 돌아가고, 그 화면이 안내를 띄운다.
                    // 워치 기록은 그 직전 화면이 되돌아오자마자 여기로 다시 보내는 URL
                    // 입력이라 흐름을 통째로 닫는다.
                    uiState.errorMessage != null ->
                        navController.leavePlaceImportIf(uiState.source.isWalkRecord)

                    uiState.extraction is ExtractionState.Success ->
                        navController.navigate(PlaceImportRoute.PLACE) {
                            popUpTo(PlaceImportRoute.LOADING) { inclusive = true }
                            launchSingleTop = true
                        }
                }
            }

            PlaceImportLoadingScreen(
                source = uiState.source,
                onCancel = {
                    viewModel.cancelExtraction()
                    navController.leavePlaceImportIf(uiState.source.isWalkRecord)
                },
            )
        }

        composable(PlaceImportRoute.PLACE) { entry ->
            val viewModel = sharedPlaceImportViewModel(navController, entry)
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            // 시스템 뒤로가기도 상단 화살표와 같은 곳으로 나가야 한다. 기본 동작은 아래
            // URL 입력으로 되돌려 로딩을 다시 태운다.
            BackHandler(enabled = uiState.source.isWalkRecord) {
                navController.popBackStack(MoaMapRoute.PlaceImport.route, inclusive = true)
            }

            PlaceImportPlaceScreen(
                source = uiState.source,
                places = uiState.places,
                selectedPlaceIds = uiState.selectedPlaceIds,
                canProceed = uiState.canProceed,
                errorMessage = uiState.errorMessage,
                // 워치 기록은 아래에 URL 입력이 깔려 있고 그 화면이 곧장 되돌려 보내므로
                // 한 칸 pop 하지 않고 흐름을 통째로 닫는다.
                onBackClick = { navController.leavePlaceImportIf(uiState.source.isWalkRecord) },
                onErrorShown = viewModel::consumeError,
                onPlaceClick = viewModel::togglePlace,
                // 장소 화면을 백스택에 남겨둔다. 로딩 중 취소하면 보던 목록으로 돌아와야 한다.
                onRetryClick = {
                    viewModel.startExtraction()
                    navController.navigate(PlaceImportRoute.LOADING)
                },
                onNextClick = { navController.navigate(PlaceImportRoute.EDIT) },
            )
        }

        composable(PlaceImportRoute.EDIT) { entry ->
            val viewModel = sharedPlaceImportViewModel(navController, entry)
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            val places = uiState.selectedPlaces
            if (places.isEmpty()) {
                LaunchedEffect(Unit) {
                    navController.popBackStack(PlaceImportRoute.URL, inclusive = false)
                }
            } else {
                PlaceImportEditScreen(
                    places = places,
                    onBackClick = navController::popBackStack,
                    // TODO: 장소 편집 화면은 다음 이슈에서 연결한다.
                    onEditClick = {},
                    onNextClick = { navController.navigate(PlaceImportRoute.MAP) },
                )
            }
        }

        composable(PlaceImportRoute.MAP) { entry ->
            val viewModel = sharedPlaceImportViewModel(navController, entry)
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            /**
             * 흐름을 떠나기로 했는지. 아래 빈 목록 가드를 잠그는 용도다.
             *
             * 그래프를 pop 하면 거기 스코프한 ViewModel 이 비워지는데, 이 화면은 종료 애니메이션
             * 동안 컴포지션에 남아 재구성된다. 그때 빈 ViewModel 이 새로 만들어져 고른 장소가
             * 사라진 것처럼 보이고, 가드가 그걸 복원으로 오해해 URL 입력으로 되돌려버린다.
             *
             * 프로세스가 재생성되면 이 값도 함께 사라져야 가드가 제 일을 하므로 저장하지 않는다.
             */
            var leaving by remember { mutableStateOf(false) }

            // 한 곳이라도 등록됐으면 흐름을 끝내고 모음 화면으로 돌려보낸다.
            // 하나도 못 넣은 경우에는 결과가 채워지지 않아 이 화면에 남고 안내만 뜬다.
            LaunchedEffect(uiState.saveResult) {
                if (uiState.saveResult != null) {
                    leaving = true
                    navController.popBackStack(MoaMapRoute.PlaceImport.route, inclusive = true)
                }
            }

            val places = uiState.selectedPlaces
            if (places.isEmpty() && !leaving) {
                // 프로세스가 재생성되면 ViewModel 은 비는데 백스택은 복원되어 이 화면부터 살아날 수 있다.
                // 고른 장소가 없으면 보여줄 것이 없으므로 흐름의 처음으로 돌려보낸다.
                LaunchedEffect(Unit) {
                    navController.popBackStack(PlaceImportRoute.URL, inclusive = false)
                }
            } else {
                PlaceImportMapScreen(
                    places = places,
                    mapsState = uiState.targetMaps,
                    selectedMapIds = uiState.selectedMapIds,
                    canSave = uiState.canSave,
                    saving = uiState.saving,
                    errorMessage = uiState.errorMessage,
                    onBackClick = navController::popBackStack,
                    onMapClick = viewModel::toggleMap,
                    onRetryMapsClick = viewModel::retryLoadMaps,
                    onSaveClick = viewModel::savePlaces,
                    onErrorShown = viewModel::consumeError,
                )
            }
        }
    }
}

/**
 * 임시. [leaveWholeFlow] 면 흐름을 통째로 닫고, 아니면 한 칸만 되돌아간다.
 *
 * 워치 기록 추천은 그래프 시작 화면인 URL 입력을 건너뛰고 들어온다. 한 칸씩 pop 하면
 * 그 URL 입력이 드러나면서 다시 로딩으로 보내버려 흐름을 빠져나갈 수 없다.
 */
private fun NavHostController.leavePlaceImportIf(leaveWholeFlow: Boolean) {
    if (leaveWholeFlow) {
        popBackStack(MoaMapRoute.PlaceImport.route, inclusive = true)
    } else {
        popBackStack()
    }
}

/** 그래프 entry 에 스코프해 네 화면이 같은 ViewModel 인스턴스를 보게 한다. */
@Composable
private fun sharedPlaceImportViewModel(
    navController: NavHostController,
    entry: NavBackStackEntry,
): PlaceImportViewModel {
    val parentEntry = remember(entry) {
        navController.getBackStackEntry(MoaMapRoute.PlaceImport.route)
    }
    return hiltViewModel(parentEntry)
}
