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

/** 하단 버튼에 마지막 카드가 가리지 않도록 확보하는 여백. */
private val BottomBarClearance = 88.dp

@Composable
internal fun PlaceImportPlaceScreen(
    places: List<ImportedPlaceUiModel>,
    selectedPlaceId: Long?,
    canProceed: Boolean,
    onBackClick: () -> Unit,
    onPlaceClick: (Long) -> Unit,
    onRetryClick: () -> Unit,
    onNextClick: () -> Unit,
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
                    title = "이 장소가 맞나요?",
                    description = "영상에서 ${places.size}곳을 찾았어요!\n맞는 곳을 골라주세요.",
                )

                Spacer(Modifier.height(PlaceImportSectionSpacing))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    places.forEach { place ->
                        ImportedPlaceCard(
                            place = place,
                            selected = place.id == selectedPlaceId,
                            onClick = { onPlaceClick(place.id) },
                        )
                    }
                }

                Spacer(Modifier.height(BottomBarClearance))
            }
        }

        PlaceImportBottomBar(modifier = Modifier.align(Alignment.BottomCenter)) {
            PlaceImportSecondaryButton(
                text = "재시도",
                onClick = onRetryClick,
            )
            PlaceImportPrimaryButton(
                text = "다음으로",
                enabled = canProceed,
                onClick = onNextClick,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

private val PreviewPlaces = listOf(
    ImportedPlaceUiModel(id = 1L, name = "커피나무", address = "서울시 동작구 369"),
    ImportedPlaceUiModel(id = 2L, name = "블루보틀 성수", address = "서울시 성동구 아차산로 7"),
    ImportedPlaceUiModel(id = 3L, name = "노티드 도넛", address = "서울시 강남구 압구정로 42길"),
)

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun PlaceImportPlaceScreenPreview() {
    MoaMapTheme {
        PlaceImportPlaceScreen(
            places = PreviewPlaces,
            selectedPlaceId = 1L,
            canProceed = true,
            onBackClick = {},
            onPlaceClick = {},
            onRetryClick = {},
            onNextClick = {},
        )
    }
}
