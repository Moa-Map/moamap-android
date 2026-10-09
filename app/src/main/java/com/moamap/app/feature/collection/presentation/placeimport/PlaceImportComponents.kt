package com.moamap.app.feature.collection.presentation.placeimport

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.moamap.app.R
import com.moamap.app.core.designsystem.component.ButtonShadowBlurRadius
import com.moamap.app.core.designsystem.component.ButtonShadowColor
import com.moamap.app.core.designsystem.component.MoaMapBackButton
import com.moamap.app.core.designsystem.component.MoaMapTopBarIconEdgePadding
import com.moamap.app.core.designsystem.component.ShadowedSurface
import com.moamap.app.core.designsystem.theme.MoaMapDimens
import com.moamap.app.core.designsystem.theme.MoaMapPrimitiveColors
import com.moamap.app.core.designsystem.theme.MoaMapTheme
import com.moamap.app.core.designsystem.theme.withDesignLineHeight
import com.moamap.app.feature.collection.domain.model.ImportedPlace
import com.moamap.app.feature.collection.domain.model.PlaceEdit

internal val PlaceImportCardShape = RoundedCornerShape(12.dp)
internal val PlaceImportButtonShape = RoundedCornerShape(8.dp)
private val SelectedPlacesCardShape = RoundedCornerShape(12.dp)

/**
 * 고른 장소 카드(시안 「추가 전_장소카드」 접힌·펼쳐진 드롭다운)의 여백. 접으면 사방 16, 펼치면 위아래 20,
 * 장소 사이는 구분선을 가운데 두고 16 이다. 장소 줄은 안쪽으로 2 더 들어간다.
 */
private val SelectedPlacesCardPadding = 16.dp
private val SelectedPlacesExpandedVerticalPadding = 20.dp
private val SelectedPlacesRowSpacing = 16.dp
private val SelectedPlaceRowInset = 2.dp

/** 이름·주소와 오른쪽 체크박스·편집하기·접기 사이. */
private val PlaceCardTrailingGap = 8.dp

private val ExpandIconSize = 14.dp

/** 편집하기 글자 위아래로 더 두는 터치 여백. */
private val EditLinkTouchPadding = 12.dp

private val CheckBoxSize = 20.dp
private val CheckBoxShape = RoundedCornerShape(4.dp)

/** 상단 바 높이. 시안 GNB 58. */
private val PlaceImportTopBarHeight = 58.dp

/** GNB 아래 첫 요소까지의 여백. 시안 「장소 가져오기」 페이지(`4362:34880`) 기준. */
internal val PlaceImportContentTopSpacing = 20.dp

/** 안내 문구와 그 아래 블록 사이 간격. */
internal val PlaceImportSectionSpacing = 20.dp

@Composable
internal fun PlaceImportTopBar(
    onBackClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(PlaceImportTopBarHeight),
    ) {
        MoaMapBackButton(
            onClick = onBackClick,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = MoaMapTopBarIconEdgePadding),
        )
        Text(
            text = "장소 가져오기",
            style = MoaMapTheme.typography.title3,
            color = MoaMapPrimitiveColors.Black,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

/** 각 단계 상단의 제목 + 설명. 피그마상 좌측으로 4dp 더 들어가 있다. */
@Composable
internal fun PlaceImportHeader(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title,
            style = MoaMapTheme.typography.subtitle1,
            color = MoaMapTheme.colors.textNormal,
        )
        Text(
            text = description,
            style = MoaMapTheme.typography.body2,
            color = MoaMapTheme.colors.textNormal,
        )
    }
}

/** 화면 하단에 고정되는 버튼 행. */
@Composable
internal fun PlaceImportBottomBar(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = MoaMapDimens.ScreenHorizontalPadding, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
internal fun PlaceImportPrimaryButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PlaceImportButton(
        text = text,
        backgroundColor = if (enabled) {
            MoaMapTheme.colors.primary
        } else {
            MoaMapPrimitiveColors.Gray200
        },
        enabled = enabled,
        onClick = onClick,
        horizontalPadding = 10.dp,
        modifier = modifier,
    )
}

/** 재시도처럼 내용 크기만 차지하는 보조 버튼. */
@Composable
internal fun PlaceImportSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PlaceImportButton(
        text = text,
        backgroundColor = MoaMapPrimitiveColors.Gray200,
        enabled = true,
        onClick = onClick,
        horizontalPadding = 24.dp,
        modifier = modifier,
    )
}

@Composable
private fun PlaceImportButton(
    text: String,
    backgroundColor: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    horizontalPadding: Dp,
    modifier: Modifier = Modifier,
) {
    ShadowedSurface(
        modifier = modifier,
        shape = PlaceImportButtonShape,
        color = backgroundColor,
        shadowBlurRadius = ButtonShadowBlurRadius,
        shadowColor = ButtonShadowColor,
        onClick = if (enabled) onClick else null,
    ) {
        Box(
            modifier = Modifier.padding(horizontal = horizontalPadding, vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                // 버튼 높이는 padding + 글자 line height 로 정해진다. 기본 Trim 을 끄지 않으면
                // 글자 높이만 잡혀 디자인보다 버튼이 낮아진다.
                style = MoaMapTheme.typography.button0.withDesignLineHeight(),
                color = MoaMapTheme.colors.textWhite,
                maxLines = 1,
            )
        }
    }
}

/** 선택된 카드를 감싸는 파란 테두리. 선택 안 된 카드는 테두리가 없다. */
@Composable
internal fun selectedCardBorder(selected: Boolean): BorderStroke? =
    if (selected) BorderStroke(width = 1.dp, color = MoaMapTheme.colors.primary) else null

/**
 * 장소와 지도 선택에 함께 쓰는 체크박스.
 *
 * 선택 시 파랑으로 채우고 흰 체크를, 선택 전에는 회색 테두리만 보여준다.
 * 고를 수 없으면([enabled] = false) 회색으로 채운다 - 모음 편집에서 방장인 지도 등.
 */
@Composable
internal fun PlaceImportCheckBox(
    checked: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Box(
        modifier = modifier
            .size(CheckBoxSize)
            .clip(CheckBoxShape)
            .then(
                if (checked) {
                    Modifier.background(MoaMapTheme.colors.primary)
                } else if (!enabled) {
                    Modifier
                        .background(MoaMapPrimitiveColors.Gray50)
                        .border(width = 1.dp, color = MoaMapPrimitiveColors.Gray100, shape = CheckBoxShape)
                } else {
                    Modifier.border(
                        width = 1.dp,
                        color = MoaMapPrimitiveColors.Gray100,
                        shape = CheckBoxShape,
                    )
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            Icon(
                painter = painterResource(R.drawable.ic_check),
                contentDescription = null,
                tint = MoaMapTheme.colors.textWhite,
                modifier = Modifier.size(CheckBoxSize),
            )
        }
    }
}

/**
 * 추출된 장소 후보 카드. 시안 「추가 전_장소카드」 선택·미선택(`4183:34212`).
 *
 * 장소를 여러 개 고를 수 있어 지도 카드와 같이 파란 테두리 + 체크박스로 표시한다. 링크에서는 장소 사진을
 * 가져올 수 없어 이름·주소만 둔다.
 */
@Composable
internal fun ImportedPlaceCard(
    place: ImportedPlace,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ShadowedSurface(
        modifier = modifier.fillMaxWidth(),
        shape = PlaceImportCardShape,
        color = MoaMapPrimitiveColors.White,
        border = selectedCardBorder(selected),
        // 등록 키가 없는 후보는 눌러도 선택되지 않는다. 눌리는 것처럼 보이지 않게 클릭 자체를 뗀다.
        onClick = onClick.takeIf { place.savable },
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(PlaceCardTrailingGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlaceLabels(
                name = place.name,
                address = place.displayAddress,
                modifier = Modifier.weight(1f),
            )
            PlaceImportCheckBox(checked = selected)
        }
    }
}

/**
 * 편집 단계에 나열되는 장소 카드. 시안 「추가 전_장소카드」 편집가능.
 *
 * 이미 고른 장소를 다시 보여주는 것이라 카드 자체는 눌리지 않고 편집하기만 눌린다.
 * 사진·태그·메모 중 하나라도 넣은 장소는 「편집하기」를 회색으로 바꿔 편집했다고 알린다(시안 `4362:34992`).
 */
@Composable
internal fun EditablePlaceCard(
    place: ImportedPlace,
    edit: PlaceEdit?,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ShadowedSurface(
        modifier = modifier.fillMaxWidth(),
        shape = PlaceImportCardShape,
        color = MoaMapPrimitiveColors.White,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(PlaceCardTrailingGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlaceLabels(
                name = place.name,
                address = place.displayAddress,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "편집하기",
                style = MoaMapTheme.typography.button4,
                color = if (edit != null && !edit.isEmpty) {
                    MoaMapTheme.colors.textAlternative
                } else {
                    MoaMapTheme.colors.statusAlert
                },
                textDecoration = TextDecoration.Underline,
                maxLines = 1,
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onEditClick,
                    )
                    .padding(vertical = EditLinkTouchPadding),
            )
        }
    }
}

/**
 * 지도 선택 화면 상단에 고정되는, 앞 단계에서 고른 장소 카드.
 *
 * 장소를 여러 개 고를 수 있어 접었을 때는 첫 장소만 보여주고 나머지는 개수로 알린다.
 * 펼치면 고른 장소를 모두 나열한다. 하나만 골랐으면 펼칠 것이 없어 토글을 내보내지 않는다.
 */
@Composable
internal fun SelectedPlacesCard(
    places: List<ImportedPlace>,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val first = places.firstOrNull() ?: return
    val rest = places.drop(1)

    ShadowedSurface(
        modifier = modifier.fillMaxWidth(),
        shape = SelectedPlacesCardShape,
        color = MoaMapPrimitiveColors.Yellow50,
        // 이 카드만 그림자가 8% 다.
        shadowColor = MoaMapPrimitiveColors.Black.copy(alpha = 0.08f),
        border = BorderStroke(width = 1.dp, color = MoaMapPrimitiveColors.Yellow500),
    ) {
        Column(
            modifier = Modifier
                .then(
                    if (rest.isEmpty()) {
                        Modifier
                    } else {
                        Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onToggleExpand,
                        )
                    },
                )
                .padding(
                    horizontal = SelectedPlacesCardPadding,
                    vertical = if (expanded) SelectedPlacesExpandedVerticalPadding else SelectedPlacesCardPadding,
                ),
            verticalArrangement = Arrangement.spacedBy(SelectedPlacesRowSpacing),
        ) {
            SelectedPlaceRow(place = first) {
                if (rest.isNotEmpty()) {
                    SelectedPlacesToggle(expanded = expanded, remainingCount = rest.size)
                }
            }

            if (expanded) {
                rest.forEach { place ->
                    HorizontalDivider(
                        thickness = 1.dp,
                        color = MoaMapPrimitiveColors.Yellow200,
                    )
                    SelectedPlaceRow(place = place)
                }
            }
        }
    }
}

@Composable
private fun SelectedPlaceRow(
    place: ImportedPlace,
    modifier: Modifier = Modifier,
    trailingContent: @Composable () -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = SelectedPlaceRowInset),
        horizontalArrangement = Arrangement.spacedBy(PlaceCardTrailingGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlaceLabels(
            name = place.name,
            address = place.displayAddress,
            modifier = Modifier.weight(1f),
        )
        trailingContent()
    }
}

/** 첫 장소 오른쪽에 붙는 `외 n개의 장소` 접기/펼치기. */
@Composable
private fun SelectedPlacesToggle(
    expanded: Boolean,
    remainingCount: Int,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "외 ${remainingCount}개의 장소",
            style = MoaMapTheme.typography.caption0,
            color = MoaMapTheme.colors.textAssistive,
            maxLines = 1,
        )
        Icon(
            painter = painterResource(R.drawable.ic_arrow_right),
            contentDescription = if (expanded) "고른 장소 접기" else "고른 장소 모두 보기",
            tint = MoaMapTheme.colors.textAssistive,
            modifier = Modifier
                .size(ExpandIconSize)
                .rotate(if (expanded) -90f else 90f),
        )
    }
}

/**
 * 이름·주소 두 줄. 시안의 사이 4 는 글자가 아니라 줄 높이 상자(이름 20.8·주소 15.6) 사이 값이라,
 * 줄 높이 여백을 살려야([withDesignLineHeight]) 간격과 카드 높이가 시안과 같다.
 */
@Composable
private fun PlaceLabels(
    name: String,
    address: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = name,
            style = MoaMapTheme.typography.subtitle2.withDesignLineHeight(),
            color = MoaMapTheme.colors.textNormal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = address,
            style = MoaMapTheme.typography.caption0.withDesignLineHeight(),
            color = MoaMapTheme.colors.textNormal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
