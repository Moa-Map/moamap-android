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
import com.moamap.app.feature.collection.domain.model.EditedPlace
import com.moamap.app.feature.collection.domain.model.PlaceImportSource
import com.moamap.app.feature.collection.presentation.placeimport.ExtractionState
import com.moamap.app.feature.collection.presentation.placeimport.PlaceImportEditDetailScreen
import com.moamap.app.feature.collection.presentation.placeimport.PlaceImportEditScreen
import com.moamap.app.feature.collection.presentation.placeimport.PlaceImportLoadingScreen
import com.moamap.app.feature.collection.presentation.placeimport.PlaceImportMapScreen
import com.moamap.app.feature.collection.presentation.placeimport.PlaceImportPlaceScreen
import com.moamap.app.feature.collection.presentation.placeimport.PlaceImportRoute
import com.moamap.app.feature.collection.presentation.placeimport.PlaceImportSearchScreen
import com.moamap.app.feature.collection.presentation.placeimport.PlaceImportUrlScreen
import com.moamap.app.feature.collection.presentation.placeimport.PlaceImportViewModel

/**
 * 링크로 장소를 가져오는 5단계 흐름. URL 입력 → 로딩 → 장소 선택 → 편집 → 지도 선택.
 *
 * 편집 단계는 목록과 장소 하나를 고치는 상세 화면으로 나뉜다. 인스타그램은 편집 목록 없이 장소 선택에서 바로
 * 상세 화면으로 가고, [지도에 추가하기]는 지도 선택으로 간다. 장소를 하나도 못 찾으면 장소 선택 자리에 안내가
 * 뜨고, 「직접 장소 검색하기」로 카카오 장소 검색에서 고른 장소 하나가 장소 선택에 다시 뜬다.
 *
 * 모든 화면이 그래프 back stack entry 에 스코프한 [PlaceImportViewModel] 하나를 공유한다.
 * 흐름을 벗어나면 ViewModel 이 함께 정리되므로 다음에 다시 들어와도 이전 입력이 남지 않는다.
 *
 * 새 지도 만들기에서 들어오면 지도 선택 단계가 없다. 넣을 지도가 아직 없어서, 편집이 끝나면
 * 그 결과를 [onImportedForNewMap] 으로 넘기고 흐름을 닫는다. 서버에는 아무것도 저장하지 않는다.
 */
internal fun NavGraphBuilder.placeImportGraph(
    navController: NavHostController,
    onImportedForNewMap: (List<EditedPlace>) -> Unit,
    /** 저장을 마쳐 흐름을 닫을 때. 돌아간 화면이 띄울 완료 토스트를 준다. */
    onImportDone: (toast: String) -> Unit,
) {
    navigation(
        route = MoaMapRoute.PlaceImport.route,
        startDestination = PlaceImportRoute.URL,
        arguments = listOf(
            navArgument(MoaMapRoute.PlaceImport.ARG_SOURCE) { type = NavType.StringType },
            navArgument(MoaMapRoute.PlaceImport.ARG_URL) {
                type = NavType.StringType
                defaultValue = ""
            },
            navArgument(MoaMapRoute.PlaceImport.ARG_FOR_NEW_MAP) {
                type = NavType.BoolType
                defaultValue = false
            },
        ),
    ) {
        composable(PlaceImportRoute.URL) { entry ->
            val viewModel = sharedPlaceImportViewModel(navController, entry)
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            PlaceImportUrlScreen(
                source = uiState.source,
                url = uiState.url,
                linkInvalid = uiState.linkInvalid,
                errorMessage = uiState.errorMessage,
                onUrlChange = viewModel::updateUrl,
                onBackClick = navController::popBackStack,
                onErrorShown = viewModel::consumeError,
                onSubmit = {
                    if (viewModel.submitUrl()) navController.navigate(PlaceImportRoute.LOADING)
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
                    // 실패하면 직전 화면으로 돌아가고, 그 화면이 안내(링크 탓이면 입력칸 오류)를 띄운다.
                    uiState.errorMessage != null || uiState.linkInvalid -> navController.popBackStack()

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
                // 인스타그램은 이 화면에서 바로 편집하므로 편집 목록을 거치지 않는다.
                onNextClick = {
                    val instagram = uiState.source == PlaceImportSource.Instagram
                    navController.navigate(if (instagram) PlaceImportRoute.MAP else PlaceImportRoute.EDIT)
                },
                edits = uiState.edits,
                onEditClick = { placeId -> navController.navigate(PlaceImportRoute.editDetail(placeId)) },
                placesFromSearch = uiState.placesFromSearch,
                // 링크 입력으로 돌아간다. 입력해 둔 링크는 그대로라 고쳐서 다시 엔터만 누르면 된다.
                onRelinkClick = { navController.popBackStack(PlaceImportRoute.URL, inclusive = false) },
                onDirectSearchClick = { navController.navigate(PlaceImportRoute.SEARCH) },
            )
        }

        composable(PlaceImportRoute.SEARCH) { entry ->
            val viewModel = sharedPlaceImportViewModel(navController, entry)
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            PlaceImportSearchScreen(
                query = uiState.searchQuery,
                search = uiState.search,
                onQueryChange = viewModel::updateSearchQuery,
                onRetryClick = viewModel::retrySearch,
                onCandidateClick = { candidate ->
                    viewModel.selectSearchedPlace(candidate)
                    // 못 찾음·검색 화면을 지우고 고른 장소 하나로 장소 고르기를 새로 연다.
                    navController.navigate(PlaceImportRoute.PLACE) {
                        popUpTo(PlaceImportRoute.PLACE) { inclusive = true }
                    }
                },
                onBackClick = navController::popBackStack,
            )
        }

        composable(PlaceImportRoute.EDIT) { entry ->
            val viewModel = sharedPlaceImportViewModel(navController, entry)
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val forNewMap = remember(entry) { navController.isPlaceImportForNewMap() }

            // 새 지도로 넘기고 흐름을 닫는 중인지. 지도 선택 화면의 같은 이름 값과 같은 이유로 둔다.
            var leaving by remember { mutableStateOf(false) }

            val places = uiState.selectedPlaces
            if (places.isEmpty() && !leaving) {
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
                    onNextClick = {
                        if (forNewMap) {
                            leaving = true
                            onImportedForNewMap(uiState.selectedEntries)
                        } else {
                            navController.navigate(PlaceImportRoute.MAP)
                        }
                    },
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
                    uiState.doneToast?.let(onImportDone)
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
                    source = uiState.source,
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

/** 새 지도 만들기 화면에서 들어온 흐름인지. 그래프 entry 의 인자로 받는다. */
private fun NavHostController.isPlaceImportForNewMap(): Boolean =
    getBackStackEntry(MoaMapRoute.PlaceImport.route)
        .arguments
        ?.getBoolean(MoaMapRoute.PlaceImport.ARG_FOR_NEW_MAP)
        ?: false

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
