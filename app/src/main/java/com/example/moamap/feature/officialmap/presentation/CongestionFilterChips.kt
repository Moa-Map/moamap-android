package com.example.moamap.feature.officialmap.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.moamap.core.designsystem.theme.MoaMapPrimitiveColors
import com.example.moamap.core.designsystem.theme.MoaMapTheme
import com.example.moamap.feature.officialmap.domain.model.CongestionLevel

/** 필터로 노출하는 레벨. 붐빔 → 여유 순서는 Figma를 따른다. */
private val FILTER_LEVELS = listOf(
    CongestionLevel.BUSY,
    CongestionLevel.SLIGHTLY_BUSY,
    CongestionLevel.NORMAL,
    CongestionLevel.RELAXED,
)

@Composable
internal fun CongestionFilterChips(
    selectedLevel: CongestionLevel?,
    onLevelClick: (CongestionLevel?) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "ALL") {
            CongestionFilterChip(
                label = "전체",
                level = null,
                selected = selectedLevel == null,
                onClick = { onLevelClick(null) },
            )
        }
        items(FILTER_LEVELS, key = { it.name }) { level ->
            CongestionFilterChip(
                label = level.label,
                level = level,
                selected = selectedLevel == level,
                onClick = { onLevelClick(level) },
            )
        }
    }
}

@Composable
private fun CongestionFilterChip(
    label: String,
    level: CongestionLevel?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (selected) MoaMapPrimitiveColors.Gray800 else MoaMapPrimitiveColors.White,
        shadowElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (level != null) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(level.color, CircleShape),
                )
            }
            Text(
                text = label,
                style = MoaMapTheme.typography.button3,
                color = if (selected) MoaMapTheme.colors.textWhite else MoaMapTheme.colors.textNormal,
            )
        }
    }
}
