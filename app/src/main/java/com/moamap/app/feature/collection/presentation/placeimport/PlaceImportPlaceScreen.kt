package com.moamap.app.feature.collection.presentation.placeimport

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
import com.moamap.app.core.designsystem.component.ErrorSnackbar
import com.moamap.app.core.designsystem.theme.MoaMapDimens
import com.moamap.app.core.designsystem.theme.MoaMapTheme
import com.moamap.app.feature.collection.domain.model.ImportedPlace
import com.moamap.app.feature.collection.domain.model.PlaceImportSource

/** 하단 버튼에 마지막 카드가 가리지 않도록 확보하는 여백. */
private val BottomBarClearance = 88.dp

@Composable
internal fun PlaceImportPlaceScreen(
    source: PlaceImportSource,
    places: List<ImportedPlace>,
    selectedPlaceIds: Set<String>,
    canProceed: Boolean,
    errorMessage: String?,
    onBackClick: () -> Unit,
    onPlaceClick: (String) -> Unit,
    onRetryClick: () -> Unit,
    onNextClick: () -> Unit,
    onErrorShown: () -> Unit,
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
                    title = when (source) {
                        PlaceImportSource.Instagram -> "이 장소가 맞나요?"
                        PlaceImportSource.MapShare -> "지도의 장소들을 불러왔어요"
                    },
                    description = when (source) {
                        PlaceImportSource.Instagram ->
                            "영상에서 ${places.size}곳을 찾았어요!\n맞는 곳을 골라주세요."

                        PlaceImportSource.MapShare ->
                            "추가하고 싶지 않은 장소들은 선택 해제를 해주세요"
                    },
                )

                Spacer(Modifier.height(PlaceImportSectionSpacing))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    places.forEach { place ->
                        ImportedPlaceCard(
                            place = place,
                            selected = place.id in selectedPlaceIds,
                            onClick = { onPlaceClick(place.id) },
                        )
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
                // 외부 지도는 리스트를 통째로 읽어온 것이라 다시 뽑아도 같은 결과다.
                // 후보를 추려내는 인스타그램에서만 재시도가 의미를 갖는다.
                if (source == PlaceImportSource.Instagram) {
                    PlaceImportSecondaryButton(
                        text = "재시도",
                        onClick = onRetryClick,
                    )
                }
                PlaceImportPrimaryButton(
                    text = "다음으로",
                    enabled = canProceed,
                    onClick = onNextClick,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

private val PreviewPlaces = listOf(
    ImportedPlace(id = "1", name = "커피나무", address = "서울시 동작구 369"),
    ImportedPlace(id = "2", name = "블루보틀 성수", address = "서울시 성동구 아차산로 7"),
    ImportedPlace(id = "3", name = "노티드 도넛", address = "서울시 강남구 압구정로 42길"),
)

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun PlaceImportPlaceScreenPreview() {
    MoaMapTheme {
        PlaceImportPlaceScreen(
            source = PlaceImportSource.Instagram,
            places = PreviewPlaces,
            selectedPlaceIds = setOf("1", "3"),
            canProceed = true,
            errorMessage = null,
            onBackClick = {},
            onPlaceClick = {},
            onRetryClick = {},
            onNextClick = {},
            onErrorShown = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun PlaceImportPlaceScreenMapSharePreview() {
    MoaMapTheme {
        PlaceImportPlaceScreen(
            source = PlaceImportSource.MapShare,
            places = PreviewPlaces,
            selectedPlaceIds = PreviewPlaces.mapTo(mutableSetOf()) { it.id },
            canProceed = true,
            errorMessage = null,
            onBackClick = {},
            onPlaceClick = {},
            onRetryClick = {},
            onNextClick = {},
            onErrorShown = {},
        )
    }
}
