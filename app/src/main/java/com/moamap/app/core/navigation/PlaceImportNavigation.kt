package com.moamap.app.core.navigation

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
import com.moamap.app.feature.collection.presentation.placeimport.ExtractionState
import com.moamap.app.feature.collection.presentation.placeimport.PlaceImportEditDetailScreen
import com.moamap.app.feature.collection.presentation.placeimport.PlaceImportEditScreen
import com.moamap.app.feature.collection.presentation.placeimport.PlaceImportLoadingScreen
import com.moamap.app.feature.collection.presentation.placeimport.PlaceImportMapScreen
import com.moamap.app.feature.collection.presentation.placeimport.PlaceImportPlaceScreen
import com.moamap.app.feature.collection.presentation.placeimport.PlaceImportRoute
import com.moamap.app.feature.collection.presentation.placeimport.PlaceImportUrlScreen
import com.moamap.app.feature.collection.presentation.placeimport.PlaceImportViewModel

/**
 * 링크로 장소를 가져오는 5단계 흐름. URL 입력 → 로딩 → 장소 선택 → 편집 → 지도 선택.
 *
 * 편집 단계는 목록과 장소 하나를 고치는 상세 화면으로 나뉜다.
 *
 * 모든 화면이 그래프 back stack entry 에 스코프한 [PlaceImportViewModel] 하나를 공유한다.
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
                    uiState.errorMessage != null -> navController.popBackStack()

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
                    navController.popBackStack()
                },
            )
        }

        composable(PlaceImportRoute.PLACE) { entry ->
            val viewModel = sharedPlaceImportViewModel(navController, entry)
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            PlaceImportPlaceScreen(
                source = uiState.source,
                places = uiState.places,
                selectedPlaceIds = uiState.selectedPlaceIds,
                canProceed = uiState.canProceed,
                errorMessage = uiState.errorMessage,
                onBackClick = navController::popBackStack,
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
                    edits = uiState.edits,
                    onBackClick = navController::popBackStack,
                    onEditClick = { placeId ->
                        navController.navigate(PlaceImportRoute.editDetail(placeId))
                    },
                    onNextClick = { navController.navigate(PlaceImportRoute.MAP) },
                )
            }
        }

        composable(
            route = PlaceImportRoute.EDIT_DETAIL,
            arguments = listOf(
                navArgument(PlaceImportRoute.ARG_PLACE_ID) { type = NavType.StringType },
            ),
        ) { entry ->
            val viewModel = sharedPlaceImportViewModel(navController, entry)
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            val placeId = entry.arguments?.getString(PlaceImportRoute.ARG_PLACE_ID)
            val place = uiState.selectedPlaces.firstOrNull { place -> place.id == placeId }

            // 프로세스가 재생성되면 ViewModel 은 비는데 백스택은 이 화면부터 살아날 수 있다.
            // 고칠 장소를 못 찾으면 보여줄 것이 없으므로 흐름의 처음으로 돌려보낸다.
            if (place == null) {
                LaunchedEffect(Unit) {
                    navController.popBackStack(PlaceImportRoute.URL, inclusive = false)
                }
            } else {
                PlaceImportEditDetailScreen(
                    place = place,
                    edit = uiState.editOf(place),
                    onBackClick = navController::popBackStack,
                    onTagsChange = { tags -> viewModel.updateEditTags(place.id, tags) },
                    onMemoChange = { memo -> viewModel.updateEditMemo(place.id, memo) },
                    onAddPhoto = { uri -> viewModel.addEditPhoto(place.id, uri) },
                    onRemovePhoto = { uri -> viewModel.removeEditPhoto(place.id, uri) },
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
