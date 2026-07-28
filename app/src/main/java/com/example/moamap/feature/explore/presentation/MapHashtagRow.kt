package com.example.moamap.feature.explore.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.moamap.core.designsystem.theme.MoaMapPrimitiveColors
import com.example.moamap.core.designsystem.theme.MoaMapTheme

/**
 * 지도 카드의 해시태그 한 줄.
 *
 * 태그 개수는 지도마다 다르므로 [maxVisible] 개까지만 보여주고 나머지는 "..." 로 접는다.
 * 한 줄을 넘기지 않는 게 우선이라 줄바꿈은 하지 않는다.
 */
@Composable
fun MapHashtagRow(
    hashtags: List<String>,
    maxVisible: Int,
    modifier: Modifier = Modifier,
) {
    if (hashtags.isEmpty()) return

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        hashtags.take(maxVisible).forEach { hashtag ->
            Text(
                text = "# $hashtag",
                style = MoaMapTheme.typography.caption0,
                color = MoaMapPrimitiveColors.Blue800,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
        }
        if (hashtags.size > maxVisible) {
            Text(
                text = "...",
                style = MoaMapTheme.typography.caption0,
                color = MoaMapPrimitiveColors.Blue800,
                maxLines = 1,
            )
        }
    }
}
