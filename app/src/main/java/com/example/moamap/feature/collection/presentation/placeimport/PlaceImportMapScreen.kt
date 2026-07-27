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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.moamap.core.designsystem.theme.MoaMapDimens
import com.example.moamap.core.designsystem.theme.MoaMapTheme
import com.example.moamap.feature.collection.CollectionMapCard
import com.example.moamap.feature.collection.CollectionMapUiModel

/** 하단 버튼에 마지막 카드가 가리지 않도록 확보하는 여백. */
private val BottomBarClearance = 88.dp

@Composable
internal fun PlaceImportMapScreen(
    place: ImportedPlaceUiModel,
    maps: List<CollectionMapUiModel>,
    selectedMapIds: Set<Long>,
    canSave: Boolean,
    onBackClick: () -> Unit,
    onMapClick: (Long) -> Unit,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
                SelectedPlaceCard(place = place)

                Spacer(Modifier.height(PlaceImportSectionSpacing))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    maps.forEach { map ->
                        CollectionMapCard(
                            map = map,
                            onClick = { onMapClick(map.id) },
                            trailingContent = {
                                PlaceImportSelectionIndicator(
                                    selected = map.id in selectedMapIds,
                                )
                            },
                        )
                    }
                }

                Spacer(Modifier.height(BottomBarClearance))
            }
        }

        PlaceImportBottomBar(modifier = Modifier.align(Alignment.BottomCenter)) {
            PlaceImportPrimaryButton(
                text = "저장하기",
                enabled = canSave,
                onClick = onSaveClick,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

private val PreviewMaps = listOf(
    CollectionMapUiModel(id = 11L, title = "내 지도", placeCount = "128곳"),
    CollectionMapUiModel(id = 12L, title = "성수 카페 투어", placeCount = "24곳"),
    CollectionMapUiModel(id = 13L, title = "주말 데이트", placeCount = "8곳"),
)

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun PlaceImportMapScreenPreview() {
    MoaMapTheme {
        PlaceImportMapScreen(
            place = ImportedPlaceUiModel(id = 1L, name = "커피나무", address = "서울시 동작구 369"),
            maps = PreviewMaps,
            selectedMapIds = setOf(12L),
            canSave = true,
            onBackClick = {},
            onMapClick = {},
            onSaveClick = {},
        )
    }
}
