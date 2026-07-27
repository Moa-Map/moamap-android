package com.example.moamap.feature.mapdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.moamap.R
import com.example.moamap.core.designsystem.theme.MoaMapPrimitiveColors
import com.example.moamap.core.designsystem.theme.MoaMapTheme

private val RoleBadgeShape = RoundedCornerShape(999.dp)

@Composable
internal fun MapDetailTopBar(
    mapTitle: String,
    roleLabel: String,
    bookmarked: Boolean,
    is3d: Boolean,
    onBackClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    on3dToggleClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(MoaMapTheme.colors.backgroundSecondary),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = 8.dp)
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

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = 68.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = mapTitle,
                style = MoaMapTheme.typography.title3,
                color = MoaMapTheme.colors.textNormal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
            Text(
                text = roleLabel,
                style = MoaMapTheme.typography.caption0,
                color = MoaMapPrimitiveColors.Blue600,
                modifier = Modifier
                    .border(
                        width = 1.dp,
                        color = MoaMapPrimitiveColors.Blue600,
                        shape = RoleBadgeShape,
                    )
                    .background(
                        color = MoaMapPrimitiveColors.Blue50,
                        shape = RoleBadgeShape,
                    )
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = (-12).dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            MapDetail3dToggle(
                is3d = is3d,
                onClick = on3dToggleClick,
            )

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clickable(onClick = onBookmarkClick),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(
                        if (bookmarked) {
                            R.drawable.ic_bookmark_filled
                        } else {
                            R.drawable.ic_bookmark_outline
                        },
                    ),
                    contentDescription = "북마크",
                    tint = if (bookmarked) {
                        MoaMapPrimitiveColors.Blue500
                    } else {
                        MoaMapPrimitiveColors.Gray100
                    },
                    modifier = Modifier.size(32.dp),
                )
            }
        }
    }
}

/** 지도를 2D(수직 시점)와 3D(입체 건물) 사이로 전환하는 토글. */
@Composable
private fun MapDetail3dToggle(
    is3d: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Text(
        text = if (is3d) "3D" else "2D",
        style = MoaMapTheme.typography.caption0,
        color = if (is3d) MoaMapPrimitiveColors.Blue600 else MoaMapTheme.colors.textAssistive,
        modifier = modifier
            .background(
                color = if (is3d) MoaMapPrimitiveColors.Blue50 else MoaMapPrimitiveColors.Gray50,
                shape = RoleBadgeShape,
            )
            .border(
                width = 1.dp,
                color = if (is3d) MoaMapPrimitiveColors.Blue600 else MoaMapPrimitiveColors.Gray100,
                shape = RoleBadgeShape,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    )
}
