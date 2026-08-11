package com.example.moamap.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.moamap.R
import com.example.moamap.core.designsystem.theme.MoaMapPrimitiveColors
import com.example.moamap.core.designsystem.theme.MoaMapTheme

private val ActionMenuWidth = 172.dp
private val ActionMenuRowHeight = 50.dp

/** [ActionMenu] 한 줄. */
@Immutable
internal data class ActionMenuItem(
    @DrawableRes val iconRes: Int,
    val label: String,
    val onClick: () -> Unit,
)

/** 줄 사이에 구분선을 넣는 팝업 메뉴. 프로필 메뉴와 사진 소스 선택이 같은 모양을 쓴다. */
@Composable
internal fun ActionMenu(
    items: List<ActionMenuItem>,
    cornerRadius: Dp,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(cornerRadius)

    // 높이는 줄 수에 따라 늘어나므로 고정하지 않고 Column 이 정하게 둔다.
    Box(modifier = modifier.width(ActionMenuWidth)) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .compatibleShadow(
                    shape = shape,
                    blurRadius = 5.dp,
                    color = MoaMapPrimitiveColors.Black.copy(alpha = 0.12f),
                ),
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(MoaMapTheme.colors.backgroundSecondary)
                .border(
                    width = 1.dp,
                    color = MoaMapPrimitiveColors.Blue600,
                    shape = shape,
                )
                .padding(horizontal = 4.dp),
        ) {
            items.forEachIndexed { index, item ->
                if (index > 0) {
                    HorizontalDivider(
                        thickness = 1.dp,
                        color = MoaMapTheme.colors.lineNormal,
                    )
                }
                ActionMenuRow(
                    iconRes = item.iconRes,
                    label = item.label,
                    onClick = item.onClick,
                )
            }
        }
    }
}

@Composable
private fun ActionMenuRow(
    @DrawableRes iconRes: Int,
    label: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(ActionMenuRowHeight)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = MoaMapTheme.colors.textNormal,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            style = MoaMapTheme.typography.body2,
            color = MoaMapTheme.colors.textNormal,
            modifier = Modifier.weight(1f),
        )
        Icon(
            painter = painterResource(R.drawable.ic_arrow_right),
            contentDescription = null,
            tint = MoaMapTheme.colors.textNormal,
            modifier = Modifier.size(20.dp),
        )
    }
}

/** 사진을 어디서 가져올지 고르는 메뉴. */
@Composable
internal fun ImageSourceMenu(
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ActionMenu(
        items = listOf(
            ActionMenuItem(R.drawable.ic_photo_camera, "카메라", onCameraClick),
            ActionMenuItem(R.drawable.ic_gallery, "갤러리", onGalleryClick),
        ),
        cornerRadius = 12.dp,
        modifier = modifier,
    )
}

@Preview(showBackground = true)
@Composable
private fun ImageSourceMenuPreview() {
    MoaMapTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MoaMapTheme.colors.backgroundSecondary),
            contentAlignment = Alignment.Center,
        ) {
            ImageSourceMenu(
                onCameraClick = {},
                onGalleryClick = {},
            )
        }
    }
}
