package com.moamap.app.feature.collection.presentation.placeimport

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.moamap.app.R
import com.moamap.app.core.designsystem.component.ErrorSnackbar
import com.moamap.app.core.designsystem.component.MoaSymbolLogo
import com.moamap.app.core.designsystem.component.ShadowedSurface
import com.moamap.app.core.designsystem.theme.MoaMapDimens
import com.moamap.app.core.designsystem.theme.MoaMapPrimitiveColors
import com.moamap.app.core.designsystem.theme.MoaMapTheme
import com.moamap.app.core.designsystem.theme.withDesignLineHeight
import com.moamap.app.feature.collection.domain.model.ImportedPlace
import com.moamap.app.feature.collection.domain.model.PlaceEdit
import com.moamap.app.feature.collection.domain.model.PlaceImportSource

/** 하단 버튼에 마지막 카드가 가리지 않도록 확보하는 여백. 인스타그램은 버튼이 둘이라 더 띄운다. */
private val BottomBarClearance = 88.dp
private val StackedButtonsClearance = 146.dp

/** 「장소를 찾지 못했어요」 묶음의 위 끝. 시안 화면 위 300 에서 상단 바(상태 표시줄 44 + 58)를 뺀 값이다. */
private val NoPlaceNoticeTop = 198.dp
private val NoPlaceLogoWidth = 100.dp
private const val NO_PLACE_LOGO_ALPHA = 0.6f

/**
 * 링크에서 찾은 장소 고르기.
 *
 * 인스타그램은 시안 「인스타그램 장소 찾기 - 장소 선택」대로 체크박스 없이 파란 테두리로 고르고, 고른 카드에만
 * 「편집하기」를 둔다. 아래에는 [지도에 추가하기]와 「다시 링크 입력하기」.
 *
 * 외부 지도는 시안 「장소 가져오기 - 장소 선택」대로 전부 고른 채 체크박스로 빼고 [다음으로]. 위치를 못 찾은
 * 장소가 있으면 몇 곳 중 몇 곳을 찾았는지 알리고, 목록 위 노란 카드를 펼쳐 못 찾은 이름을 보인다.
 *
 * 둘 다 하나도 못 찾았으면 「장소를 찾지 못했어요」 안내로 바뀐다(시안 「검색결과 0」, 외부 지도도 같게 - 이슈 #390).
 */
@Composable
internal fun PlaceImportPlaceScreen(
    source: PlaceImportSource,
    places: List<ImportedPlace>,
    selectedPlaceIds: Set<String>,
    canProceed: Boolean,
    errorMessage: String?,
    onBackClick: () -> Unit,
    onPlaceClick: (String) -> Unit,
    onNextClick: () -> Unit,
    onErrorShown: () -> Unit,
    modifier: Modifier = Modifier,
    /** 장소 id 로 찾는 편집값. 「편집하기」 색을 가른다(인스타그램). */
    edits: Map<String, PlaceEdit> = emptyMap(),
    /** 「편집하기」. 장소 하나를 고치는 화면으로 간다(인스타그램). */
    onEditClick: (String) -> Unit = {},
    /** 「직접 장소 검색하기」로 고른 장소라 링크에서 찾았다는 안내가 맞지 않는다. */
    placesFromSearch: Boolean = false,
    /** 외부 지도에서 위치를 못 찾아 목록에서 빠진 장소 이름. */
    unmatchedNames: List<String> = emptyList(),
    /** 「다시 링크 입력하기」·[다른 링크 입력하기]. 링크 입력으로 돌아간다. */
    onRelinkClick: () -> Unit = {},
    /** 「직접 장소 검색하기」. 장소 검색으로 간다. */
    onDirectSearchClick: () -> Unit = {},
) {
    val instagram = source == PlaceImportSource.Instagram
    if (places.isEmpty()) {
        NoPlaceFoundScreen(
            source = source,
            onBackClick = onBackClick,
            onRelinkClick = onRelinkClick,
            onDirectSearchClick = onDirectSearchClick,
            modifier = modifier,
        )
        return
    }

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
                    title = if (instagram || placesFromSearch) "이 장소가 맞나요?" else "지도의 장소들을 불러왔어요",
                    description = when {
                        placesFromSearch -> "검색한 장소를 편집하거나\n바로 지도에 추가해 보세요."
                        instagram -> "영상에서 ${places.size}곳을 찾았어요!\n맞는 곳을 골라주세요."
                        unmatchedNames.isEmpty() -> MAP_SHARE_DESELECT_GUIDE
                        else -> "지도의 ${places.size + unmatchedNames.size}곳 중 ${places.size}곳을 찾았어요\n" +
                            MAP_SHARE_DESELECT_GUIDE
                    },
                )

                Spacer(Modifier.height(PlaceImportSectionSpacing))

                if (unmatchedNames.isNotEmpty()) {
                    UnmatchedPlacesCard(names = unmatchedNames)
                    Spacer(Modifier.height(PlaceImportSectionSpacing))
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    places.forEach { place ->
                        ImportedPlaceCard(
                            place = place,
                            selected = place.id in selectedPlaceIds,
                            onClick = { onPlaceClick(place.id) },
                            showsCheckBox = !instagram,
                            onEditClick = if (instagram) ({ onEditClick(place.id) }) else null,
                            edit = edits[place.id],
                        )
                    }
                }

                Spacer(Modifier.height(if (instagram) StackedButtonsClearance else BottomBarClearance))
            }
        }

        // 안내가 버튼에 가리지 않도록 버튼 위에 쌓는다.
        Column(modifier = Modifier.align(Alignment.BottomCenter)) {
            ErrorSnackbar(
                message = errorMessage,
                onShown = onErrorShown,
            )
            if (instagram) {
                PlaceImportStackedButtons(
                    primaryText = "지도에 추가하기",
                    primaryEnabled = canProceed,
                    onPrimaryClick = onNextClick,
                    secondaryText = "다시 링크 입력하기",
                    onSecondaryClick = onRelinkClick,
                )
            } else {
                PlaceImportBottomBar {
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
}

/**
 * 링크에서 장소를 하나도 못 찾았을 때. 시안 「인스타그램 장소 찾기 - 검색결과 0」: 흐린 로고 100 아래 subtitle1
 * 제목과 body1 설명, 아래 [다른 링크 입력하기]·「직접 장소 검색하기」. 외부 지도도 같고 설명만 다르다.
 */
@Composable
private fun NoPlaceFoundScreen(
    source: PlaceImportSource,
    onBackClick: () -> Unit,
    onRelinkClick: () -> Unit,
    onDirectSearchClick: () -> Unit,
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
                    .padding(top = NoPlaceNoticeTop),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                MoaSymbolLogo(
                    bodyWidth = NoPlaceLogoWidth,
                    modifier = Modifier.alpha(NO_PLACE_LOGO_ALPHA),
                    contentDescription = null,
                )
                Column(
                    modifier = Modifier.semantics(mergeDescendants = true) {},
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "장소를 찾지 못했어요",
                        style = MoaMapTheme.typography.subtitle1,
                        color = MoaMapTheme.colors.textNormal,
                    )
                    Text(
                        text = when (source) {
                            PlaceImportSource.Instagram -> "다른 릴스 링크를 입력하거나\n직접 검색해보세요"
                            PlaceImportSource.MapShare -> "다른 지도 링크를 입력하거나\n직접 검색해보세요"
                        },
                        style = MoaMapTheme.typography.body1.withDesignLineHeight(),
                        color = MoaMapTheme.colors.textAlternative,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        PlaceImportStackedButtons(
            primaryText = "다른 링크 입력하기",
            primaryEnabled = true,
            onPrimaryClick = onRelinkClick,
            secondaryText = "직접 장소 검색하기",
            onSecondaryClick = onDirectSearchClick,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

/**
 * 위치를 못 찾은 장소 안내. 시안 「장소 가져오기 - 장소 선택」(`5147:74002` 접힘·`5152:74131` 펼침): 노란 카드에
 * 경고 아이콘·「n곳은 위치를 찾지 못했어요」·이유, 누르면 아래 8 에 못 찾은 이름을 흰 알약으로 늘어놓는다.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun UnmatchedPlacesCard(names: List<String>, modifier: Modifier = Modifier) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    ShadowedSurface(
        modifier = modifier.fillMaxWidth(),
        shape = PlaceImportCardShape,
        color = MoaMapPrimitiveColors.Yellow50,
        border = BorderStroke(width = 1.dp, color = MoaMapPrimitiveColors.Yellow500),
        onClick = { expanded = !expanded },
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(R.drawable.ic_notification_alert),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(12.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = "${names.size}곳은 위치를 찾지 못했어요",
                        style = MoaMapTheme.typography.subtitle4.withDesignLineHeight(),
                        color = MoaMapTheme.colors.textNormal,
                    )
                    Text(
                        text = "이름이 다르거나 검색되지 않는 장소에요",
                        style = MoaMapTheme.typography.caption0.withDesignLineHeight(),
                        color = MoaMapTheme.colors.textAlternative,
                    )
                }
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_down),
                    contentDescription = if (expanded) "못 찾은 장소 접기" else "못 찾은 장소 보기",
                    tint = MoaMapTheme.colors.textNormal,
                    modifier = Modifier
                        .size(20.dp)
                        .rotate(if (expanded) 180f else 0f),
                )
            }
            if (expanded) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    names.forEach { name ->
                        Text(
                            text = name,
                            style = MoaMapTheme.typography.caption0.withDesignLineHeight(),
                            color = MoaMapPrimitiveColors.Gray900,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .background(MoaMapPrimitiveColors.White, CircleShape)
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                        )
                    }
                }
            }
        }
    }
}

/** 외부 지도 장소 고르기 설명의 공통 줄. */
private const val MAP_SHARE_DESELECT_GUIDE = "추가하고 싶지 않은 장소들은 선택 해제를 해주세요"

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
            selectedPlaceIds = setOf("1"),
            canProceed = true,
            errorMessage = null,
            onBackClick = {},
            onPlaceClick = {},
            onNextClick = {},
            onErrorShown = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun PlaceImportNoPlacePreview() {
    MoaMapTheme {
        PlaceImportPlaceScreen(
            source = PlaceImportSource.Instagram,
            places = emptyList(),
            selectedPlaceIds = emptySet(),
            canProceed = false,
            errorMessage = null,
            onBackClick = {},
            onPlaceClick = {},
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
            onNextClick = {},
            onErrorShown = {},
            unmatchedNames = listOf("나고미칸", "을지로 골목 술집"),
        )
    }
}
