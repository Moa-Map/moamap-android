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
import com.example.moamap.feature.collection.domain.model.ImportedPlace
import com.example.moamap.feature.collection.domain.model.PlaceEdit

/** 하단 버튼에 마지막 카드가 가리지 않도록 확보하는 여백. */
private val BottomBarClearance = 88.dp

/**
 * 앞 단계에서 고른 장소의 정보를 고칠 기회를 주는 화면.
 *
 * 편집은 선택 사항이라 아무것도 고치지 않고 그대로 넘어갈 수 있다.
 */
@Composable
internal fun PlaceImportEditScreen(
    places: List<ImportedPlace>,
    edits: Map<String, PlaceEdit>,
    onBackClick: () -> Unit,
    onEditClick: (String) -> Unit,
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
                    title = "장소를 편집하시겠어요?",
                    description = "정보를 수정할 장소를 편집해보세요",
                )

                Spacer(Modifier.height(PlaceImportSectionSpacing))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    places.forEach { place ->
                        EditablePlaceCard(
                            place = place,
                            edit = edits[place.id],
                            onEditClick = { onEditClick(place.id) },
                        )
                    }
                }

                Spacer(Modifier.height(BottomBarClearance))
            }
        }

        PlaceImportBottomBar(modifier = Modifier.align(Alignment.BottomCenter)) {
            PlaceImportPrimaryButton(
                text = "다음으로",
                enabled = true,
                onClick = onNextClick,
                modifier = Modifier.weight(1f),
            )
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
private fun PlaceImportEditScreenPreview() {
    MoaMapTheme {
        PlaceImportEditScreen(
            places = PreviewPlaces,
            edits = mapOf("2" to PlaceEdit(tags = listOf("카페"), memo = "창가 자리")),
            onBackClick = {},
            onEditClick = {},
            onNextClick = {},
        )
    }
}
