package com.example.moamap.feature.mypage

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
private val ActionMenuHeight = 100.dp
private val ActionMenuRowHeight = 50.dp

@Composable
internal fun ProfileActionMenu(
    @DrawableRes firstIconRes: Int,
    firstLabel: String,
    onFirstClick: () -> Unit,
    @DrawableRes secondIconRes: Int,
    secondLabel: String,
    onSecondClick: () -> Unit,
    cornerRadius: Dp,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .width(ActionMenuWidth)
            .height(ActionMenuHeight),
    ) {
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
                .matchParentSize()
                .clip(shape)
                .background(MoaMapTheme.colors.backgroundSecondary)
                .border(
                    width = 1.dp,
                    color = MoaMapPrimitiveColors.Blue600,
                    shape = shape,
                )
                .padding(horizontal = 4.dp),
        ) {
            ProfileActionMenuRow(
                iconRes = firstIconRes,
                label = firstLabel,
                onClick = onFirstClick,
            )
            ProfileActionMenuRow(
                iconRes = secondIconRes,
                label = secondLabel,
                onClick = onSecondClick,
            )
        }

        HorizontalDivider(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 4.dp),
            thickness = 1.dp,
            color = MoaMapTheme.colors.lineNormal,
        )
    }
}

@Composable
private fun ProfileActionMenuRow(
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

@Composable
internal fun ProfileImageSourceMenu(
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ProfileActionMenu(
        firstIconRes = R.drawable.ic_photo_camera,
        firstLabel = "카메라",
        onFirstClick = onCameraClick,
        secondIconRes = R.drawable.ic_gallery,
        secondLabel = "갤러리",
        onSecondClick = onGalleryClick,
        cornerRadius = 12.dp,
        modifier = modifier,
    )
}

@Preview(showBackground = true)
@Composable
private fun ProfileImageSourceMenuPreview() {
    MoaMapTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MoaMapTheme.colors.backgroundSecondary),
            contentAlignment = Alignment.Center,
        ) {
            ProfileImageSourceMenu(
                onCameraClick = {},
                onGalleryClick = {},
            )
        }
    }
}
