package com.moamap.app.feature.officialmap.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.moamap.app.core.designsystem.theme.MoaMapPrimitiveColors
import com.moamap.app.core.designsystem.theme.MoaMapTheme
import com.moamap.app.feature.officialmap.domain.model.AreaCongestion
import com.moamap.app.feature.officialmap.domain.model.CongestionLevel
import com.moamap.app.feature.officialmap.domain.model.DensityArea
import kotlin.math.roundToInt

/** 카드 통계 한 칸. 라벨은 최다 연령대·성별에 따라 바뀐다. */
internal data class AreaStat(val label: String, val value: String)

internal fun AreaCongestion?.toAreaStats(): List<AreaStat> {
    if (this == null) return emptyList()
    return buildList {
        populationMin?.let { add(AreaStat("인구", formatPopulation(it))) }
        dominantAge?.let { add(AreaStat("${it.label} 비율", "${it.rate.roundToInt()}%")) }
        dominantGender?.let { add(AreaStat("${it.label} 비율", "${it.rate.roundToInt()}%")) }
    }
}

/**
 * 실제 API는 100명대 지역도 내려주므로 만 단위로 나눠 버리면 대부분이 "약 0만 명"이 된다.
 * 1만 미만은 명 단위로, 그 이상은 만 단위로 쓰되 자투리를 소수 한 자리까지 남긴다.
 */
private fun formatPopulation(population: Long): String {
    if (population < 10_000) return "약 %,d명".format(population)
    val tenThousands = population / 10_000.0
    return if (tenThousands % 1.0 == 0.0) "약 ${tenThousands.toInt()}만 명"
    else "약 ${"%.1f".format(tenThousands)}만 명"
}

@Composable
internal fun AreaInfoCard(
    area: DensityArea,
    modifier: Modifier = Modifier,
) {
    val congestion = area.congestion
    val level = congestion?.level ?: CongestionLevel.UNKNOWN
    val stats = congestion.toAreaStats()

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MoaMapPrimitiveColors.White,
        shadowElevation = 5.dp,
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AreaInfoHeader(name = area.name, level = level)

            if (stats.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    stats.forEach { stat ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = stat.label,
                                style = MoaMapTheme.typography.caption0,
                                color = MoaMapTheme.colors.textAlternative,
                                maxLines = 1,
                            )
                            Text(
                                text = stat.value,
                                style = MoaMapTheme.typography.body3,
                                color = MoaMapTheme.colors.textAlternative,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }

            congestion?.message?.let { message ->
                Text(
                    text = message,
                    style = MoaMapTheme.typography.caption0,
                    color = MoaMapTheme.colors.textAssistive,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun AreaInfoHeader(name: String, level: CongestionLevel) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(level.color, CircleShape),
        )
        Text(
            text = name,
            style = MoaMapTheme.typography.subtitle2,
            color = MoaMapTheme.colors.textNormal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            // 장소명이 길어도 태그를 밀어내지 않게 남는 폭만 차지한다.
            modifier = Modifier.weight(1f, fill = false),
        )
        CongestionTag(level = level)
    }
}

@Composable
private fun CongestionTag(level: CongestionLevel) {
    val colors = level.tagColors
    Box(
        modifier = Modifier
            .background(colors.background, CircleShape)
            .border(1.dp, colors.border, CircleShape)
            .padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        Text(
            text = level.label,
            style = MoaMapTheme.typography.caption0,
            color = colors.content,
            maxLines = 1,
        )
    }
}
