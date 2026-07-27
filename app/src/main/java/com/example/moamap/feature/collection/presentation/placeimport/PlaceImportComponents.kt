package com.example.moamap.feature.collection.presentation.placeimport

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.moamap.R
import com.example.moamap.core.designsystem.component.ButtonShadowBlurRadius
import com.example.moamap.core.designsystem.component.ButtonShadowColor
import com.example.moamap.core.designsystem.component.ShadowedSurface
import com.example.moamap.core.designsystem.theme.MoaMapDimens
import com.example.moamap.core.designsystem.theme.MoaMapPrimitiveColors
import com.example.moamap.core.designsystem.theme.MoaMapTheme

internal val PlaceImportCardShape = RoundedCornerShape(12.dp)
internal val PlaceImportButtonShape = RoundedCornerShape(8.dp)
private val SelectedPlaceCardShape = RoundedCornerShape(16.dp)

/** 썸네일 크기. 장소 카드와 선택된 장소 카드가 같은 값을 쓴다. */
private val PlaceThumbnailSize = 64.dp

/** GNB 아래 첫 요소까지의 여백. 피그마 393x852 기준 좌표에서 계산했다. */
internal val PlaceImportContentTopSpacing = 23.dp

/** 안내 문구와 그 아래 블록 사이 간격. */
internal val PlaceImportSectionSpacing = 24.dp

@Composable
internal fun PlaceImportTopBar(
    onBackClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 8.dp)
                .size(48.dp)
                .clickable(onClick = onBackClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_left),
                contentDescription = "뒤로가기",
                tint = MoaMapTheme.colors.textNormal,
                modifier = Modifier.size(24.dp),
            )
        }
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
                style = MoaMapTheme.typography.button0,
                color = MoaMapTheme.colors.textWhite,
                maxLines = 1,
            )
        }
    }
}

/**
 * 선택 표시.
 *
 * 장소 선택과 지도 선택이 같은 표시를 쓴다. 장소 쪽은 피그마에 표식이 없어서
 * 지도 선택과 동일한 형태로 맞췄다.
 */
@Composable
internal fun PlaceImportSelectionIndicator(
    selected: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(20.dp)
            .border(width = 1.dp, color = MoaMapTheme.colors.primary, shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .background(MoaMapTheme.colors.primary, CircleShape),
            )
        }
    }
}

/** 추출된 장소 후보 카드. */
@Composable
internal fun ImportedPlaceCard(
    place: ImportedPlaceUiModel,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ShadowedSurface(
        modifier = modifier.fillMaxWidth(),
        shape = PlaceImportCardShape,
        color = MoaMapPrimitiveColors.White,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlaceThumbnail(cornerRadius = 4.dp)
            PlaceLabels(
                name = place.name,
                address = place.address,
                modifier = Modifier.weight(1f),
            )
            PlaceImportSelectionIndicator(selected = selected)
        }
    }
}

/** 지도 선택 화면 상단에 고정되는, 앞 단계에서 고른 장소 카드. */
@Composable
internal fun SelectedPlaceCard(
    place: ImportedPlaceUiModel,
    modifier: Modifier = Modifier,
) {
    ShadowedSurface(
        modifier = modifier.fillMaxWidth(),
        shape = SelectedPlaceCardShape,
        color = MoaMapPrimitiveColors.Yellow50,
        // 이 카드만 그림자가 8% 다.
        shadowColor = MoaMapPrimitiveColors.Black.copy(alpha = 0.08f),
        border = BorderStroke(width = 1.dp, color = MoaMapPrimitiveColors.Yellow500),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlaceThumbnail(
                cornerRadius = 12.dp,
                backgroundColor = MoaMapPrimitiveColors.Blue50,
                borderColor = MoaMapPrimitiveColors.Black.copy(alpha = 0.2f),
            )
            PlaceLabels(
                name = place.name,
                address = place.address,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

// TODO: 장소 썸네일 이미지는 API 연동 시 채운다.
@Composable
private fun PlaceThumbnail(
    cornerRadius: Dp,
    backgroundColor: Color = MoaMapPrimitiveColors.Yellow50,
    borderColor: Color? = null,
) {
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = Modifier
            .size(PlaceThumbnailSize)
            .clip(shape)
            .background(backgroundColor)
            .let { modifier ->
                if (borderColor == null) {
                    modifier
                } else {
                    modifier.border(width = 1.dp, color = borderColor, shape = shape)
                }
            },
    )
}

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
            style = MoaMapTheme.typography.subtitle2,
            color = MoaMapTheme.colors.textNormal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = address,
            style = MoaMapTheme.typography.caption0,
            color = MoaMapTheme.colors.textNormal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
