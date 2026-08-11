package com.example.moamap.feature.collection.presentation.placeimport

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.moamap.core.designsystem.component.ErrorSnackbar
import com.example.moamap.core.designsystem.theme.MoaMapDimens
import com.example.moamap.core.designsystem.theme.MoaMapTheme
import com.example.moamap.feature.collection.CollectionMapCard
import com.example.moamap.feature.collection.MapsStateContent
import com.example.moamap.feature.collection.domain.model.ImportedPlace
import com.example.moamap.feature.collection.domain.model.MyMap
import com.example.moamap.feature.collection.presentation.MyMapsState
import com.example.moamap.feature.collection.toPrivateUiModel

/** 하단 버튼에 마지막 카드가 가리지 않도록 확보하는 여백. */
private val BottomBarClearance = 88.dp

@Composable
internal fun PlaceImportMapScreen(
    places: List<ImportedPlace>,
    mapsState: MyMapsState,
    selectedMapIds: Set<Long>,
    canSave: Boolean,
    saving: Boolean,
    errorMessage: String?,
    onBackClick: () -> Unit,
    onMapClick: (Long) -> Unit,
    onRetryMapsClick: () -> Unit,
    onSaveClick: () -> Unit,
    onErrorShown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 고른 장소를 펼쳐 봤는지는 이 화면에서만 쓰고 끝나므로 ViewModel 까지 올리지 않는다.
    var placesExpanded by rememberSaveable { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MoaMapTheme.colors.backgroundSecondary)
            .statusBarsPadding(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            PlaceImportTopBar(onBackClick = onBackClick)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = MoaMapDimens.ScreenHorizontalPadding),
            ) {
                Spacer(Modifier.height(PlaceImportContentTopSpacing))

                PlaceImportHeader(
                    title = "어디에 저장할까요?",
                    description = "이 장소를 추가할 지도를 선택해주세요!",
                )

                Spacer(Modifier.height(PlaceImportSectionSpacing))

                // 앞 단계에서 고른 장소를 다시 보여준다.
                SelectedPlacesCard(
                    places = places,
                    expanded = placesExpanded,
                    onToggleExpand = { placesExpanded = !placesExpanded },
                )

                Spacer(Modifier.height(PlaceImportSectionSpacing))

                MapsStateContent(
                    state = mapsState,
                    emptyMessage = "저장할 지도가 없어요",
                    onRetryClick = onRetryMapsClick,
                ) { targetMaps ->
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        targetMaps.forEach { map ->
                            // 한 장소를 여러 지도에 넣을 수 있어 체크박스로 복수 선택한다.
                            val selected = map.id in selectedMapIds
                            CollectionMapCard(
                                map = map.toPrivateUiModel(),
                                onClick = { onMapClick(map.id) },
                                border = selectedCardBorder(selected),
                                trailingContent = { PlaceImportCheckBox(checked = selected) },
                            )
                        }
                    }
                }

                Spacer(Modifier.height(BottomBarClearance))
            }
        }

        // 안내가 버튼에 가리지 않도록 버튼 위에 쌓는다.
        Column(modifier = Modifier.align(Alignment.BottomCenter)) {
            ErrorSnackbar(
                message = errorMessage,
                onShown = onErrorShown,
            )
            PlaceImportBottomBar {
                PlaceImportPrimaryButton(
                    text = if (saving) "저장하는 중.." else "저장하기",
                    enabled = canSave,
                    onClick = onSaveClick,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

private val PreviewMaps = listOf(
    MyMap(11L, "내 지도", null, memberCount = 1, placeCount = 0, official = false, personal = true),
    MyMap(12L, "성수 카페 투어", null, memberCount = 1, placeCount = 8, official = false, personal = false),
    MyMap(13L, "주말 데이트", null, memberCount = 1, placeCount = 3, official = false, personal = false),
)

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun PlaceImportMapScreenPreview() {
    MoaMapTheme {
        PlaceImportMapScreen(
            places = listOf(
                ImportedPlace(id = "1", name = "커피나무", address = "서울시 동작구 369"),
                ImportedPlace(id = "2", name = "블루보틀 성수", address = "서울시 성동구 아차산로 7"),
            ),
            mapsState = MyMapsState.Success(PreviewMaps),
            selectedMapIds = setOf(12L),
            canSave = true,
            saving = false,
            errorMessage = null,
            onBackClick = {},
            onMapClick = {},
            onRetryMapsClick = {},
            onSaveClick = {},
            onErrorShown = {},
        )
    }
}
