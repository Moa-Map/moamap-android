package com.example.moamap.feature.explore.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.moamap.R
import com.example.moamap.core.designsystem.component.ListCardShadowBlurRadius
import com.example.moamap.core.designsystem.component.ListCardShadowColor
import com.example.moamap.core.designsystem.component.ShadowedSurface
import com.example.moamap.core.designsystem.theme.MoaMapTheme
import com.example.moamap.feature.explore.domain.model.CommunityMap

private const val MAX_VISIBLE_HASHTAGS = 3

/**
 * 탐색 탭의 커뮤니티 지도 목록 카드.
 */
@Composable
fun CommunityMapCard(
    map: CommunityMap,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ShadowedSurface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        shadowBlurRadius = ListCardShadowBlurRadius,
        shadowColor = ListCardShadowColor,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MapThumbnail(imageUrl = map.imageUrl, size = 90.dp)

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.End,
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = map.title,
                        style = MoaMapTheme.typography.subtitle2,
                        color = MoaMapTheme.colors.textNormal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    MapHashtagRow(
                        hashtags = map.hashtags,
                        maxVisible = MAX_VISIBLE_HASHTAGS,
                    )
                }
                MemberCountMeta(memberCount = map.memberCount)
            }
        }
    }
}

@Composable
private fun MemberCountMeta(memberCount: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_person),
            contentDescription = "참여 인원",
            tint = MoaMapTheme.colors.textAssistive,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = formatMemberCount(memberCount),
            style = MoaMapTheme.typography.caption0,
            color = MoaMapTheme.colors.textAssistive,
            maxLines = 1,
        )
    }
}

@Preview(showBackground = true, widthDp = 393)
@Composable
private fun CommunityMapCardPreview() {
    MoaMapTheme {
        CommunityMapCard(
            map = CommunityMap(
                id = 1L,
                title = "서울 팝업스토어 맵",
                imageUrl = null,
                hashtags = listOf("맛집", "데이트코스", "데이트", "카페"),
                memberCount = 2312,
                joined = false,
            ),
            onClick = {},
            modifier = Modifier.padding(20.dp),
        )
    }
}
