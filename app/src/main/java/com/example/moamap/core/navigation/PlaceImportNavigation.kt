package com.example.moamap.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.example.moamap.feature.collection.presentation.placeimport.ExtractionState
import com.example.moamap.feature.collection.presentation.placeimport.PlaceImportLoadingScreen
import com.example.moamap.feature.collection.presentation.placeimport.PlaceImportMapScreen
import com.example.moamap.feature.collection.presentation.placeimport.PlaceImportPlaceScreen
import com.example.moamap.feature.collection.presentation.placeimport.PlaceImportRoute
import com.example.moamap.feature.collection.presentation.placeimport.PlaceImportUrlScreen
import com.example.moamap.feature.collection.presentation.placeimport.PlaceImportViewModel

/**
 * 인스타그램 URL 로 장소를 가져오는 4단계 흐름.
 *
 * 네 화면이 그래프 back stack entry 에 스코프한 [PlaceImportViewModel] 하나를 공유한다.
 * 흐름을 벗어나면 ViewModel 이 함께 정리되므로 다음에 다시 들어와도 이전 입력이 남지 않는다.
 */
internal fun NavGraphBuilder.placeImportGraph(navController: NavHostController) {
    navigation(
        route = MoaMapRoute.PlaceImport.route,
        startDestination = PlaceImportRoute.URL,
    ) {
        composable(PlaceImportRoute.URL) { entry ->
            val viewModel = sharedPlaceImportViewModel(navController, entry)
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            PlaceImportUrlScreen(
                url = uiState.url,
                canSearch = uiState.canSearch,
                onUrlChange = viewModel::updateUrl,
                onBackClick = navController::popBackStack,
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
            LaunchedEffect(uiState.extraction) {
                if (uiState.extraction is ExtractionState.Success) {
                    navController.navigate(PlaceImportRoute.PLACE) {
                        popUpTo(PlaceImportRoute.LOADING) { inclusive = true }
                    }
                }
            }

            PlaceImportLoadingScreen(
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
                places = uiState.places,
                selectedPlaceId = uiState.selectedPlaceId,
                canProceed = uiState.canProceed,
                onBackClick = navController::popBackStack,
                onPlaceClick = viewModel::selectPlace,
                onRetryClick = {
                    viewModel.startExtraction()
                    navController.navigate(PlaceImportRoute.LOADING) {
                        popUpTo(PlaceImportRoute.PLACE) { inclusive = true }
                    }
                },
                onNextClick = { navController.navigate(PlaceImportRoute.MAP) },
            )
        }

        composable(PlaceImportRoute.MAP) { entry ->
            val viewModel = sharedPlaceImportViewModel(navController, entry)
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            uiState.selectedPlace?.let { place ->
                PlaceImportMapScreen(
                    place = place,
                    maps = uiState.targetMaps,
                    selectedMapIds = uiState.selectedMapIds,
                    canSave = uiState.canSave,
                    onBackClick = navController::popBackStack,
                    onMapClick = viewModel::toggleMap,
                    // TODO: 저장 동작과 이후 이동은 다음 작업에서 연결한다.
                    onSaveClick = {},
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
