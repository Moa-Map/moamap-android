package com.moamap.app.feature.mapdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.moamap.app.R
import com.moamap.app.core.designsystem.theme.MoaMapPrimitiveColors
import com.moamap.app.core.designsystem.theme.MoaMapTheme

private val ToggleSize = DpSize(48.dp, 100.dp)
private val ToggleShape = RoundedCornerShape(20.dp)
private val FabShape = RoundedCornerShape(1000.dp)

/** 토글과 FAB 사이. 피그마의 두 좌표 차이(586 - 574)가 그대로 12dp 다. */
private val ControlsGap = 12.dp

/**
 * 지도 위 오른쪽에 세로로 놓이는 컨트롤.
 *
 * 원래 상단바에 있던 3D/2D 토글이 여기로 내려왔고, 장소 추가 버튼이 새로 붙었다.
 */
@Composable
internal fun MapDetailMapControls(
    is3d: Boolean,
    canAddPlace: Boolean,
    on3dToggleClick: () -> Unit,
    onAddPlaceClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(ControlsGap),
    ) {
        MapDimensionToggle(is3d = is3d, onClick = on3dToggleClick)
        AddPlaceButton(enabled = canAddPlace, onClick = onAddPlaceClick)
    }
}

/**
 * 3D 가 위, 2D 가 아래로 붙어 있는 세로 pill (`1841:12566`). 고른 쪽이 파랗게 채워진다.
 *
 * 테두리는 두 조각에 따로 주지 않고 바깥에 한 번만 덮는다. 조각마다 주면 맞닿는 변에
 * 선이 생겨 가운데가 갈라져 보인다 - 디자인에는 그 선이 없다.
 */
@Composable
private fun MapDimensionToggle(
    is3d: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(ToggleSize)
            .clip(ToggleShape)
            .selectableGroup(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            MapDimensionSegment(
                label = "3D",
                selected = is3d,
                // 이미 고른 쪽을 다시 눌러도 바뀔 게 없다.
                onClick = { if (!is3d) onClick() },
                modifier = Modifier.weight(1f),
            )
            MapDimensionSegment(
                label = "2D",
                selected = !is3d,
                onClick = { if (is3d) onClick() },
                modifier = Modifier.weight(1f),
            )
        }

        // 테두리는 조각 위에 덮어야 한다. 부모의 테두리는 자식보다 먼저 그려져 가려진다.
        // 그리기 전용 Box 라 터치는 그대로 아래 조각으로 내려간다.
        Box(
            modifier = Modifier
                .matchParentSize()
                .border(width = 1.dp, color = MoaMapPrimitiveColors.Blue500, shape = ToggleShape),
        )
    }
}

@Composable
private fun MapDimensionSegment(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                if (selected) {
                    MoaMapPrimitiveColors.Blue500
                } else {
                    MoaMapTheme.colors.backgroundSecondary
                },
            )
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MoaMapTheme.typography.button2,
            color = if (selected) {
                MoaMapTheme.colors.textWhite
            } else {
                MoaMapPrimitiveColors.Black
            },
        )
    }
}

/**
 * 내 위치로 이동. 지도 왼쪽 아래에 홀로 놓인다.
 *
 * [MapDetailMapControls] 에 넣지 않는다. 그건 오른쪽 아래 컬럼이고 이건 왼쪽 아래다.
 *
 * 오른쪽 컨트롤과 달리 흰 바탕에 파란 아이콘이다. 오른쪽은 지도를 바꾸는 조작이고
 * 이쪽은 보던 자리를 되돌리는 보조라, 색으로 역할을 갈라 둔다.
 *
 * 좌표를 찾는 동안에는 눌리지 않는다. 연타로 조회가 겹치면 카메라가 두 번 튄다.
 */
@Composable
internal fun MyLocationButton(
    inProgress: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .shadow(elevation = 10.dp, shape = FabShape, clip = false)
            .clip(FabShape)
            .background(MoaMapTheme.colors.backgroundSecondary)
            .clickable(enabled = !inProgress, onClick = onClick)
            .padding(8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_location),
            contentDescription = "내 위치로 이동",
            tint = if (inProgress) MoaMapPrimitiveColors.Gray100 else MoaMapPrimitiveColors.Blue500,
            modifier = Modifier.size(32.dp),
        )
    }
}

/**
 * 장소 추가.
 *
 * 참여하지 않은 지도에서는 누를 수 없다. 비활성 색은 피그마에 없어 팔레트에서 골랐다.
 */
@Composable
private fun AddPlaceButton(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .shadow(elevation = 10.dp, shape = FabShape, clip = false)
            .clip(FabShape)
            .background(
                if (enabled) MoaMapPrimitiveColors.Blue500 else MoaMapPrimitiveColors.Gray100,
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_add),
            contentDescription = "장소 추가",
            tint = if (enabled) MoaMapTheme.colors.textWhite else MoaMapPrimitiveColors.Gray50,
            modifier = Modifier.size(32.dp),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFDDE5E8, widthDp = 160, heightDp = 200)
@Composable
private fun MapDetailMapControlsPreview() {
    MoaMapTheme {
        Box(
            modifier = Modifier
                .background(Color(0xFFDDE5E8))
                .padding(20.dp),
        ) {
            MapDetailMapControls(
                is3d = false,
                canAddPlace = true,
                on3dToggleClick = {},
                onAddPlaceClick = {},
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFDDE5E8, widthDp = 160, heightDp = 200)
@Composable
private fun MapDetailMapControlsDisabledPreview() {
    MoaMapTheme {
        Box(
            modifier = Modifier
                .background(Color(0xFFDDE5E8))
                .padding(20.dp),
        ) {
            MapDetailMapControls(
                is3d = true,
                canAddPlace = false,
                on3dToggleClick = {},
                onAddPlaceClick = {},
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFDDE5E8, widthDp = 160, heightDp = 100)
@Composable
private fun MyLocationButtonPreview() {
    MoaMapTheme {
        Box(
            modifier = Modifier
                .background(Color(0xFFDDE5E8))
                .padding(20.dp),
        ) {
            MyLocationButton(inProgress = false, onClick = {})
        }
    }
}
