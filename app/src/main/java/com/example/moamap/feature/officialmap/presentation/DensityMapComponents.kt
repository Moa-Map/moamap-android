package com.example.moamap.feature.officialmap.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.moamap.R
import com.example.moamap.core.designsystem.theme.MoaMapPrimitiveColors
import com.example.moamap.core.designsystem.theme.MoaMapTheme
import com.example.moamap.feature.officialmap.domain.model.CongestionLevel
import com.example.moamap.feature.officialmap.domain.model.DensityArea

/** 혼잡도 레벨별 표시 색. TODO: 디자인 토큰 확정 시 교체 */
internal val CongestionLevel.color: Color
    get() = when (this) {
        CongestionLevel.RELAXED -> Color(0xFF34C759)
        CongestionLevel.NORMAL -> Color(0xFFFFCC00)
        CongestionLevel.SLIGHTLY_BUSY -> Color(0xFFFF9500)
        CongestionLevel.BUSY -> Color(0xFFFF3B30)
        CongestionLevel.UNKNOWN -> Color(0xFF8E8E93)
    }

@Composable
internal fun DensityMapTopBar(
    mapTitle: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp),
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
        Row(
            modifier = Modifier.align(Alignment.Center),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = mapTitle,
                style = MoaMapTheme.typography.title3,
                color = MoaMapPrimitiveColors.Black,
            )
            Box(
                modifier = Modifier
                    .background(Color(0xFFFF3B30), RoundedCornerShape(8.dp))
                    .padding(horizontal = 7.dp, vertical = 2.dp),
            ) {
                Text(
                    text = "LIVE",
                    style = MoaMapTheme.typography.caption0,
                    color = Color.White,
                )
            }
        }
    }
}

@Composable
internal fun CongestionLegend(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MoaMapTheme.colors.backgroundSecondary,
        shadowElevation = 4.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            listOf(
                CongestionLevel.RELAXED,
                CongestionLevel.NORMAL,
                CongestionLevel.SLIGHTLY_BUSY,
                CongestionLevel.BUSY,
            ).forEach { level ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(level.color, CircleShape),
                    )
                    Text(
                        text = level.label,
                        style = MoaMapTheme.typography.caption0,
                        color = MoaMapTheme.colors.textNormal,
                    )
                }
            }
        }
    }
}

@Composable
internal fun AreaInfoChip(
    area: DensityArea,
    modifier: Modifier = Modifier,
) {
    val congestion = area.congestion
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MoaMapTheme.colors.backgroundSecondary,
        shadowElevation = 6.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background((congestion?.level ?: CongestionLevel.UNKNOWN).color, CircleShape),
                )
                Text(
                    text = "${area.name} · ${(congestion?.level ?: CongestionLevel.UNKNOWN).label}",
                    style = MoaMapTheme.typography.body1,
                    color = MoaMapTheme.colors.textNormal,
                )
            }
            val detail = buildList {
                congestion?.populationMin?.let { add("약 ${it / 10000}만 명") }
                congestion?.dominantAge?.let { add("${it.label} ${it.rate.toInt()}%") }
                congestion?.dominantGender?.let { add("${it.label} ${it.rate.toInt()}%") }
            }
            if (detail.isNotEmpty()) {
                Text(
                    text = detail.joinToString(" · "),
                    style = MoaMapTheme.typography.caption0,
                    color = MoaMapTheme.colors.textAlternative,
                )
            }
            congestion?.message?.let { message ->
                Text(
                    text = message,
                    style = MoaMapTheme.typography.caption0,
                    color = MoaMapTheme.colors.textAlternative,
                )
            }
        }
    }
}
